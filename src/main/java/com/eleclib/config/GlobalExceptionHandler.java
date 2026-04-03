package com.eleclib.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Обработка необработанных исключений — показываем страницу ошибки вместо Whitelabel.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleException(Exception e, HttpServletRequest request, Model model) {
        log.error("Ошибка при обработке запроса {} {}", request.getMethod(), request.getRequestURI(), e);
        String message = e.getMessage() != null ? e.getMessage() : "Внутренняя ошибка сервера";
        if (message.length() > 200) message = message.substring(0, 200) + "...";
        model.addAttribute("errorMessage", message);
        model.addAttribute("status", 500);
        return "error";
    }
}
