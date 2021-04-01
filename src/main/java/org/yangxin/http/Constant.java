package org.yangxin.http;

public interface Constant {

    int DEFAULT_REQSUT_TIME_OUT = 100;

    int COMPUTOR_CORE = Runtime.getRuntime().availableProcessors();

    String DEFAULT_CHARSET = "UTF-8";

    String DEFAULT_PROTOCOL = "HTTP/1.1";

    /**
     * CRLF.
     */
    public static final String CRLF = "\r\n";

    /**
     * CR.
     */
    public static final byte CR = (byte) '\r';


    /**
     * LF.
     */
    public static final byte LF = (byte) '\n';


    /**
     * SP.
     */
    public static final byte SP = (byte) ' ';

    /**
     * HT.
     */
    public static final byte HT = (byte) '\t';


    /**
     * COLON.
     */
    public static final byte COLON = (byte) ':';

    /**
     * SEMI_COLON.
     */
    public static final byte SEMI_COLON = (byte) ';';

    /**
     * '?'.
     */
    public static final byte QUESTION = (byte) '?';

}
