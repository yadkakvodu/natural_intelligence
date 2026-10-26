FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1 \
    HF_HOME=/models \
    TRANSFORMERS_CACHE=/models

WORKDIR /app

# torch needs libgomp at runtime
RUN apt-get update \
 && apt-get install -y --no-install-recommends libgomp1 \
 && rm -rf /var/lib/apt/lists/*

# CPU-only torch: the default PyPI wheel pulls ~5 GB of CUDA libs (9.8 GB image vs ~2 GB)
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu \
        torch torchvision

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

# The model (~2.3 GB) is downloaded on first start, hence the long start period.
HEALTHCHECK --interval=15s --timeout=5s --start-period=600s --retries=40 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/health')" || exit 1

CMD ["python", "python_ai.py"]
