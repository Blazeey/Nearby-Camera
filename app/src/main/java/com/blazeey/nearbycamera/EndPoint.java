package com.blazeey.nearbycamera;

/**
 * Created by Blazeey on 10/1/2017.
 */

public class EndPoint {
    private String endpointId,endPointName;

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEndPointName() {
        return endPointName;
    }

    public void setEndPointName(String endPointName) {
        this.endPointName = endPointName;
    }

    public EndPoint(String endpointId, String endPointName) {
        this.endpointId = endpointId;
        this.endPointName = endPointName;
    }
}
