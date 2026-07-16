package com.ivedi.ivedikuryemanagement.delivery.api;

public enum PaymentCollectionType {
    /** Payment already collected — courier carries no money. */
    NO_COLLECTION,
    /** Courier collects cash from customer at the door. */
    CASH_ON_DELIVERY,
    /** Courier carries a card terminal. */
    CARD_ON_DELIVERY,
    /** Paid online; courier carries no money. */
    PAID_ONLINE
}
