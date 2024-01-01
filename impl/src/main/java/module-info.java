import io.github.bmarwell.sipper.api.SipClientBuilder;
import io.github.bmarwell.sipper.impl.DefaultSipClientBuilder;

module io.github.bmarwell.sipper.impl {
    requires io.github.bmarwell.sipper.api;
    requires java.net.http;
    requires org.dnsjava;
    requires org.slf4j;

    provides SipClientBuilder with
            DefaultSipClientBuilder;
}
