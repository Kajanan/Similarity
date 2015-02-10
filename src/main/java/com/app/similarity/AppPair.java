/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.app.similarity;

/**
 *
 * @author workshop
 */
public class AppPair {
    String App1;
    int AppID1;
    int AppID2;

    public AppPair(String App1,int AppID1, String App2,int AppID2 ) {
        this.App1 = App1;
        this.AppID1 = AppID1;
        this.AppID2 = AppID2;
        this.App2 = App2;
    }

    public int getAppID1() {
        return AppID1;
    }

    public void setAppID1(int AppID1) {
        this.AppID1 = AppID1;
    }

    public int getAppID2() {
        return AppID2;
    }

    public void setAppID2(int AppID2) {
        this.AppID2 = AppID2;
    }

    public AppPair(String App1, String App2) {
        this.App1 = App1;
        this.App2 = App2;
    }
    String App2;

    public String getApp1() {
        return App1;
    }

    public void setApp1(String App1) {
        this.App1 = App1;
    }

    public String getApp2() {
        return App2;
    }

    public void setApp2(String App2) {
        this.App2 = App2;
    }

}
