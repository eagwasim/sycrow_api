package com.sycrow.api.constant;

public enum PlatformAttributeNamesConstant {
    BARTER_TOKEN_CREATION_L_B_S_("BARTER_TOKEN_CREATION_L_B_S"),
    BARTER_TOKEN_CREATION_L_I_P_("BARTER_TOKEN_CREATION_L_I_P"),
    BARTER_TOKEN_WITHDRAWAL_L_B_S("BARTER_TOKEN_WITHDRAWAL_L_B_S"),
    BARTER_TOKEN_TRADE_L_B_S("BARTER_TOKEN_TRADE_L_B_S");

    private final String val;

    PlatformAttributeNamesConstant(String val) {
        this.val = val;
    }

    public String getNameForChain(String chainId) {
        return String.format("%s_%s", this.val, chainId);
    }
}
