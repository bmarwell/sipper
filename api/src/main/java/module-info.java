import io.github.bmarwell.sipper.api.SipClientBuilder;

module io.github.bmarwell.sipper.api {
    uses SipClientBuilder;

    requires static org.immutables.value.annotations;

    exports io.github.bmarwell.sipper.api;
}
