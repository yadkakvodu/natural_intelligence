package com.example.demo.dto;

public class PythonRequestDTO {
    private String requestType;
    private String syntheticU;
    private ClientProfileDTO clientProfile;
    private ParamsDTO params;

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getSyntheticU() { return syntheticU; }
    public void setSyntheticU(String syntheticU) { this.syntheticU = syntheticU; }

    public ClientProfileDTO getClientProfile() { return clientProfile; }
    public void setClientProfile(ClientProfileDTO clientProfile) { this.clientProfile = clientProfile; }

    public ParamsDTO getParams() { return params; }
    public void setParams(ParamsDTO params) { this.params = params; }
}