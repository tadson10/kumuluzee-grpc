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
package com.kumuluz.ee.grpc.processor;

import com.kumuluz.ee.grpc.annotations.GrpcService;
import com.kumuluz.ee.grpc.utils.AnnotationProcessorUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/***
 * GrpcServiceAnnotationProcessor class
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class GrpcServiceAnnotationProcessor extends AbstractProcessor {

    private static final Logger logger = Logger.getLogger(GrpcServiceAnnotationProcessor.class.getName());

    private Filer filer;
    private Messager messager;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> services;

        messager.printMessage(Diagnostic.Kind.NOTE, "Processing");
        services = roundEnv.getElementsAnnotatedWith(GrpcService.class);

        Set<String> grpcServiceNames = new HashSet<>();

        for (Element e : services) {
            logger.info(e.toString());
            grpcServiceNames.add(e.toString());
        }

        try {
            if (!grpcServiceNames.isEmpty()) {
                AnnotationProcessorUtil.writeFile(grpcServiceNames, "META-INF/services/io.grpc.BindableService", filer);
            }
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }

        return false;
    }
}
