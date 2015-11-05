package com.martiansoftware.martifacts.web;

import static com.martiansoftware.boom.Boom.request;
import java.util.Optional;
import spark.Filter;
import spark.Request;
import spark.Response;

/**
 *
 * @author mlamb
 */
public class AppRootHelper implements Filter {

    private static final String URL_KEY = AppRootHelper.class.getName() + "_URL_KEY";
    
    public static Optional<String> get() {
        return Optional.of(request().attribute(URL_KEY));
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        request.attribute(URL_KEY, request().url().replaceAll("/[^/]*$", ""));
    }
}
