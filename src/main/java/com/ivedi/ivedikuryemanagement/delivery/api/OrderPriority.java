package com.ivedi.ivedikuryemanagement.delivery.api;

public enum OrderPriority {
    /** Standard delivery — FIFO queue. */
    NORMAL,
    /** Elevated — placed before NORMAL in dispatch queue. */
    HIGH,
    /** Immediate — placed before HIGH and NORMAL. */
    URGENT;

    /** Higher value = dispatched earlier. Used for in-memory sorting of the ready queue. */
    public int sortValue() {
        return this.ordinal(); // NORMAL=0, HIGH=1, URGENT=2
    }
}
