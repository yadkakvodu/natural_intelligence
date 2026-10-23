package entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "clients")
public class Client {
    @Id
    @Column(name = "synthetic_u")
    private String syntheticU;

    @Column(name = "mainokved_code")
    private String mainOkvedCode;

    @Column(name = "cl_product_loa_mark")
    private Boolean clProductLoaMark;

    @Column(name = "cl_product_dep_mark")
    private Boolean clProductDepMark;

    @Column(name = "fts_flag")
    private Boolean ftsFlag;

    @Column(name = "fts_country_name")
    private String ftsCountryName;

    @Column(name = "revenue_company")
    private BigDecimal revenueCompany;

    @Column(name = "group_id")
    private String groupId;

    public String getSyntheticU() {
        return syntheticU;
    }

    public void setSyntheticU(String syntheticU) {
        this.syntheticU = syntheticU;
    }

    public String getMainOkvedCode() {
        return mainOkvedCode;
    }

    public void setMainOkvedCode(String mainOkvedCode) {
        this.mainOkvedCode = mainOkvedCode;
    }

    public Boolean getClProductLoaMark() {
        return clProductLoaMark;
    }

    public void setClProductLoaMark(Boolean clProductLoaMark) {
        this.clProductLoaMark = clProductLoaMark;
    }

    public Boolean getClProductDepMark() {
        return clProductDepMark;
    }

    public void setClProductDepMark(Boolean clProductDepMark) {
        this.clProductDepMark = clProductDepMark;
    }

    public Boolean getFtsFlag() {
        return ftsFlag;
    }

    public void setFtsFlag(Boolean ftsFlag) {
        this.ftsFlag = ftsFlag;
    }

    public String getFtsCountryName() {
        return ftsCountryName;
    }

    public void setFtsCountryName(String ftsCountryName) {
        this.ftsCountryName = ftsCountryName;
    }

    public BigDecimal getRevenueCompany() {
        return revenueCompany;
    }

    public void setRevenueCompany(BigDecimal revenueCompany) {
        this.revenueCompany = revenueCompany;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}