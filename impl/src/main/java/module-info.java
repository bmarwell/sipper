import io.github.bmarwell.sipper.api.SipClientBuilder;
import io.github.bmarwell.sipper.impl.DefaultSipClientBuilder;

module io.github.bmarwell.sipper.impl {
    requires io.github.bmarwell.sipper.api;

    provides SipClientBuilder with
            DefaultSipClientBuilder;
}
