package com.celfocus.hiring.kickstarter.api;

import com.celfocus.hiring.kickstarter.api.dto.CartItemInput;
import com.celfocus.hiring.kickstarter.api.dto.CartResponse;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
public interface CartAPI {
    @PostMapping("/items")
    @PreAuthorize("@authorizationService.isCartOwner(#username)")
    ResponseEntity<Void> addItemToCart(@RequestHeader("username") String username, @Valid @RequestBody CartItemInput itemInput);

    @DeleteMapping
    @PreAuthorize("@authorizationService.isCartOwner(#username)")
    ResponseEntity<Void> clearCart(@RequestHeader("username") String username);

    @GetMapping
    @PreAuthorize("@authorizationService.isCartOwner(#username)")
    ResponseEntity<CartResponse> getCart(@RequestHeader("username") String username);

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("@authorizationService.isCartOwner(#username)")
    ResponseEntity<Void> removeItemFromCart(@RequestHeader("username") String username, @PathVariable("itemId") String itemId);
}
