import os
import glob
import pymupdf  # PyMuPDF
import hashlib
from datetime import datetime
import psycopg2
from sentence_transformers import SentenceTransformer
from dotenv import load_dotenv
import easyocr

# 1. Загрузка конфигурации из .env
load_dotenv()

DB_CONFIG = {
    "dbname": os.getenv("PG_DB", "defaultdb"),
    "user": os.getenv("PG_USER", "avnadmin"),
    "password": os.getenv("PG_PASS"),
    "host": os.getenv("PG_HOST", "localhost"),
    "port": os.getenv("PG_PORT", "13266"),
    "sslmode": "require"
}

# Абсолютный путь к папке с PDF
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PDF_DIR = os.path.join(BASE_DIR, "news_pdfs")

# Инициализация OCR (используем 'ru', он также корректно считывает английские термины и цифры)
print("📥 Инициализация OCR-модуля (EasyOCR)...")
ocr_reader = easyocr.Reader(['ru'], gpu=False, download_enabled=True)

# Инициализация модели эмбеддингов
print("📥 Загрузка модели эмбеддингов BAAI/bge-m3...")
embedding_model = SentenceTransformer("BAAI/bge-m3")


def extract_text_from_pdf(pdf_path: str) -> str:
    """Извлечение текста через нейросетевой OCR по изображениям страниц."""
    doc = pymupdf.open(pdf_path)
    full_text = []

    for i, page in enumerate(doc):
        # Рендерим страницу PDF в качественное изображение
        pix = page.get_pixmap(dpi=150)
        img_bytes = pix.tobytes("png")

        # Распознаем текст с помощью EasyOCR
        results = ocr_reader.readtext(img_bytes, detail=0)
        page_text = " ".join(results)

        if page_text.strip():
            full_text.append(page_text)
        else:
            print(f"      ⚠️ Страница {i + 1}: OCR ничего не нашел.")

    return "\n".join(full_text).strip()


def compute_hash(text: str) -> str:
    """Генерирует уникальный хеш содержимого для предотвращения дубликатов."""
    return hashlib.sha256(text.encode('utf-8')).hexdigest()


def ingest_pdfs():
    conn = psycopg2.connect(**DB_CONFIG)
    cursor = conn.cursor()

    pdf_files = glob.glob(os.path.join(PDF_DIR, "*.pdf"))
    print(f"📂 Найдено PDF-файлов в папке '{PDF_DIR}': {len(pdf_files)}")

    for path in pdf_files:
        filename = os.path.basename(path)

        print(f"\n⚙️ Обработка файла (OCR): {filename}...")

        try:
            # 1. Извлечение текста через OCR
            body_text = extract_text_from_pdf(path)
            if not body_text:
                print(f"⚠️ Файл пустой или текст не удалось распознать: {filename}")
                continue

            print(f"   ✍️ Распознано символов: {len(body_text)}")
            content_hash = compute_hash(body_text)

            # 2. Проверка на дубликаты по content_hash
            cursor.execute("SELECT 1 FROM news WHERE content_hash = %s LIMIT 1;", (content_hash,))
            if cursor.fetchone():
                print(f"⏩ Пропущено (уже существует в базе): {filename}")
                continue

            # 3. Формирование полей
            news_id = f"news_{content_hash[:16]}"
            title = filename.replace(".pdf", "").replace("_", " ")
            summary = (body_text[:600] + "...") if len(body_text) > 600 else body_text
            source = "Alfa Bank Research / Private Banking"
            topics = ["Macro", "Strategy", "Private Banking"]
            is_analytics = True

            # 4. Генерация векторного эмбеддинга
            vector = embedding_model.encode(body_text[:2000]).tolist()

            # 5. Запись в таблицу public.news
            insert_sql = """
                INSERT INTO news (
                    news_id, title, published_at, source, topics, 
                    summary, body_md, is_analytics, content_hash, embedding, ingested_at
                ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s::vector, %s)
                ON CONFLICT (news_id) DO NOTHING;
            """

            cursor.execute(insert_sql, (
                news_id,
                title,
                datetime.now(),
                source,
                topics,
                summary,
                body_text,
                is_analytics,
                content_hash,
                str(vector),
                datetime.now()
            ))
            conn.commit()
            print(f"✅ Успешно загружено в БД: {filename} (ID: {news_id})")

        except Exception as e:
            conn.rollback()
            print(f"❌ Ошибка при обработке {filename}: {e}")

    cursor.close()
    conn.close()
    print("\n🎉 Ингест всех PDF-файлов успешно завершен!")


if __name__ == "__main__":
    ingest_pdfs()