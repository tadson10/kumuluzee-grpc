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
package com.kumuluz.ee.grpc;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import java.util.logging.Logger;

/**
 * GrpcExtension class.
 *
 * @author Primož Hrovat
 * @since 1.0.0
 */
@EeExtensionDef(name = "gRPC", group = "grpc")
@EeComponentDependency(EeComponentType.SERVLET)
public class GrpcExtension implements Extension {

    private static final Logger logger = Logger.getLogger(GrpcExtension.class.getName());

    @Override
    public void load() { }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        Initializer.initialize();
        logger.info("gRPC Extension initialized");
    }

    @Override
    public boolean isEnabled() {
        ConfigurationUtil confUtil = ConfigurationUtil.getInstance();
        return confUtil.getBoolean("kumuluzee.grpc.enabled").orElse(true);
    }
}
