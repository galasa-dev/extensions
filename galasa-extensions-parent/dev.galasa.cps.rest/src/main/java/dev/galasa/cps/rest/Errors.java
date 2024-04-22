/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.cps.rest;

import java.text.MessageFormat;

public enum Errors {

    ERROR_GALASA_WRONG_NUMBER_OF_PARAMETERS_IN_MESSAGE                 (6999,"GAL6999E: Failed to render message template. Not the expected number of parameters. Got ''{0}''. Expected ''{1}''"),
    ERROR_GALASA_API_SERVER_URI_DOESNT_START_WITH_REST_SCHEME          (7000,"GAL7000E: URL of ''{0}'' is invalid. It does not start with ''{1}''"),
    ERROR_GALASA_API_SERVER_URI_IS_INVALID                             (7001,"GAL7001E: URL of ''{0}'' is invalid. {1}"),
    ERROR_GALASA_CONSTRUCTED_URL_TO_REMOTE_CPS_INVALID_SYNTAX          (7002,"GAL7002E: URL ''{0}'' is of an invalid syntax. {1}"),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_FAILED_NON_OK_STATUS    (7003,"GAL7003E: Could not get the CPS property value from URL ''{0}''. Status code ''{1}'' is not 200."),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_FAILED                  (7004,"GAL7004E: Could not get the CPS property value from URL ''{0}''. Cause: {1}"),
    ERROR_GALASA_CANT_GET_JWT_TOKEN                                    (7005,"GAL7005E: Could not find the GALASA_JWT in the available configuration."),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_BAD_JSON_RETURNED       (7006,"GAL7006E: Could not get the CPS property value from URL ''{0}''. Cause: Bad json returned from the server. {1}"),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTY_TOO_FEW_OR_MANY_RETURNED(7007,"GAL7007E: Could not get the SPC property valie from URL ''${0}''. Unexpected number of results. Expected {1} got {2}"),
    ERROR_GALASA_CPS_SET_OPERATIONS_NOT_PERMITTED                      (7008,"GAL7008E: Local test runs are not permitted to set CPS properties on the remote Galasa server."),
    ERROR_GALASA_CPS_DELETE_OPERATIONS_NOT_PERMITTED                   (7009,"GAL7009E: Local test runs are not permitted to delete CPS properties on the remote Galasa server."),
    ERROR_GALASA_CPS_SHUTDOWN_FAILED                                   (7010,"GAL7010E: Galasa internal error. Rest-based CPS store failed to shut down. Cause: {0}"),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTIES_FAILED_NON_OK_STATUS  (7011,"GAL7011E: Could not get the CPS property values from URL ''{0}''. Status code ''{1}'' is not 200."),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_PROPERTIES_FAILED                (7012,"GAL7012E: Could not get the CPS property value from URL ''{0}''. Cause: {1}"),
    ERROR_GALASA_REST_CALL_TO_GET_ALL_CPS_PROPERTIES_NON_OK_STATUS     (7013,"GAL7013E: Could not get all the CPS property values from URL ''{0}''. Status code ''{1}'' is not 200."),
    ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_METADATA         (7014,"GAL7014E: Galasa server error. Payload returned is not what we expected. Metadata field is missing from a property definition."),
    ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_NAMESPACE        (7015,"GAL7015E: Galasa server error. Payload returned is not what we expected. Metadata.namespsace field is missing from a property definition."),
    ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_NAME             (7016,"GAL7016E: Galasa server error. Payload returned is not what we expected. Metadata.name field is missing from a property definition."),
    ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_DATA             (7017,"GAL7017E: Galasa server error. Payload returned is not what we expected. Data field is missing from a property definition."),
    ERROR_GALASA_CPS_PROPERTIES_RETURNED_BADLY_FORMED_VALUE            (7018,"GAL7018E: Galasa server error. Payload returned is not what we expected. Data.value field is missing from a property definition."),
    ERROR_GALASA_REST_CALL_TO_GET_ALL_CPS_NAMESPACES_NON_OK_STATUS     (7019,"GAL7019E: Could not get the namespace information from URL ''{0}''. Status code ''{1}'' is not 200."),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_NAMESPACES_FAILED                (7020,"GAL7020E: Could not get the CPS namespaces information from URL ''{0}''. Cause: {1}"),
    ERROR_GALASA_REST_CALL_TO_GET_CPS_NAMESPACES_BAD_JSON_RETURNED     (7021,"GAL7021E: Could not get the CPS namespaces value from URL ''{0}''. Cause: Bad json returned from the server. {1}"),
    ;

    private String template;
    private int expectedParameterCount;
    private Errors(int ordinal, String template ) {
        this.template = template ;
        this.expectedParameterCount = this.template.split("[{]").length-1;
    }


    public String getMessage() {
        String msg ;
        int actualParameterCount = 0;

        if (actualParameterCount!= this.expectedParameterCount) {
            msg = ERROR_GALASA_WRONG_NUMBER_OF_PARAMETERS_IN_MESSAGE.getMessage(
                actualParameterCount, 
                this.expectedParameterCount);
        } else {
            msg = this.template;
        }

        return msg;
    }

    public String getMessage(Object o1) {

        String msg ;
        int actualParameterCount = 1;

        if (actualParameterCount!= this.expectedParameterCount) {
            
            msg = ERROR_GALASA_WRONG_NUMBER_OF_PARAMETERS_IN_MESSAGE.getMessage(
                actualParameterCount, 
                this.expectedParameterCount);
        } else {
            msg = MessageFormat.format(this.template,o1);
        }

        return msg;
    }

    public String getMessage(Object o1, Object o2) {

        String msg ;
        int actualParameterCount = 2;

        if (actualParameterCount!= this.expectedParameterCount) {
            template = "Failed to render message template. Not the expected number of parameters. Got ''{0}''. Expected ''{1}''";
            msg = MessageFormat.format(template,actualParameterCount, this.expectedParameterCount);
        } else {
            msg = MessageFormat.format(this.template,o1,o2);
        }

        return msg;
    }

    public String getMessage(Object o1, Object o2, Object o3) {

        String msg ;
        int actualParameterCount = 3;

        if (actualParameterCount!= this.expectedParameterCount) {
            template = "Failed to render message template. Not the expected number of parameters. Got ''{0}''. Expected ''{1}''";
            msg = MessageFormat.format(template,actualParameterCount, this.expectedParameterCount);
        } else {
            msg = MessageFormat.format(this.template,o1,o2,o3);
        }

        return msg;
    }
}
