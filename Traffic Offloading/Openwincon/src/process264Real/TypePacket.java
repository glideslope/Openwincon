package process264Real;

import java.io.Serializable;

public enum TypePacket implements Serializable {
    request,
    feedback,
    ping,
    general,
    last
}
