/*
 *  Copyright (c) 2014-2018 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.grpc.utils;

import io.grpc.ServerInterceptor;

import java.util.List;

/***
 * GrpcServiceDef class
 *
 * Helper data holder class.
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class GrpcServiceDef {

    private String serviceName;
    private List<ServerInterceptor> serviceInterceptors;

    public boolean hasInterceptors() {
        return serviceInterceptors != null && !serviceInterceptors.isEmpty();
    }

    public List<ServerInterceptor> getServiceInterceptors() {
        return serviceInterceptors;
    }

    public GrpcServiceDef setServiceInterceptors(List<ServerInterceptor> serviceInterceptors) {
        this.serviceInterceptors = serviceInterceptors;
        return this;
    }

    public String getServiceName() {
        return serviceName;
    }

    public GrpcServiceDef setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }
}
