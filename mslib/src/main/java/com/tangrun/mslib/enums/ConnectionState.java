package com.tangrun.mslib.enums;

public enum ConnectionState {
    // initial state.
    NEW("NEW"),
    Offline("Offline"),
    Online("Online"),
    Left("Left"),
    ;

    public final String value;

    ConnectionState(String value) {
        this.value = value;
    }

    public static ConnectionState get(String value){
        for (ConnectionState state : values()) {
            if (state.value.equals(value))return state;
        }
        return NEW;
    }

    @Override
    public String toString() {
        return value;
    }
}
