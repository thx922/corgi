package com.dounine.corgi.register.api;

/**
 * Created by huanghuanlai on 2016/10/21.
 */
public class P2PRegNode implements RegNode {

    private String path;
    private String address;

    public P2PRegNode(String path, String address){
        this.path = path;
        this.address = address;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
