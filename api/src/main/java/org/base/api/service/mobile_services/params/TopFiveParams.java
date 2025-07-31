package org.base.api.service.mobile_services.params;

/**
 * Parameters for the top-five endpoint.
 */
public class TopFiveParams extends CommonParams {
    private final String transport;

    public TopFiveParams(Integer year, String quarter, String transport) {
        super(year, quarter, null, null, null);
        this.transport = transport;
    }

    public String getTransport() {
        return transport;
    }

//    @Override
//    public List<String> getValidations() {
//        List<String> validations = new ArrayList<>(super.getValidations());
//        if (transport != null && !transport.equals("99")) {
//            validations.add("transport");
//        }
//        return validations;
//    }
}
