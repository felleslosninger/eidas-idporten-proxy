package no.idporten.eidas.proxy.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.proxy.integration.specificcommunication.exception.SpecificCommunicationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Exception handling for ID-porten web.
 */
@ControllerAdvice
@Slf4j
public class WebExceptionControllerAdvice {

    @ExceptionHandler(SpecificCommunicationException.class)
    public String handleOAuth2Exception(HttpServletRequest request, HttpServletResponse response, Model model, SpecificCommunicationException exception) {
        log.error("SpecificCommunicationException occurred: {}", exception.getMessage(), exception);
        //todo return error message with light protocol https://digdir.atlassian.net/browse/ID-4233
        return "it_is_you";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(HttpServletRequest request, HttpServletResponse response, Model model, Exception e) {
        log.error("Exception occurred: {}", e.getMessage(), e);
        //todo return error message with light protocol https://digdir.atlassian.net/browse/ID-4233
        return "it_is_us";
    }

}
