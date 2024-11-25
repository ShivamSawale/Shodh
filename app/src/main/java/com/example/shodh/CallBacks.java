package com.example.shodh;


public class CallBacks<T> {

    public static int STATUS_LOADING = 1;
    public static int STATUS_SUCCESS = 2;
    public static int STATUS_EMPTY = 3;
    public static int STATUS_FAILED = 4;

    private T data;
    private int status;
    private String error;

    public CallBacks() {
    }

    public CallBacks(int status, T data, String error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    public static <T> CallBacks<T> loading() {
        return new CallBacks<>(STATUS_LOADING, null, null);
    }

    public static <T> CallBacks<T> success(T data) {
        return new CallBacks<>(STATUS_SUCCESS, data, null);
    }

    public static <T> CallBacks<T> failed(String error) {
        return new CallBacks<>(STATUS_FAILED, null, error);
    }

    public static <T> CallBacks<T> empty() {
        return new CallBacks<>(STATUS_EMPTY, null, null);
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setError(String error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
