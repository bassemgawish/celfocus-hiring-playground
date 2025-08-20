package com.celfocus.hiring.kickstarter.api;

import com.celfocus.hiring.kickstarter.api.dto.CartItemInput;
import com.celfocus.hiring.kickstarter.db.entity.CartEntity;
import com.celfocus.hiring.kickstarter.db.entity.CartItemEntity;
import com.celfocus.hiring.kickstarter.db.entity.CartItemPK;
import com.celfocus.hiring.kickstarter.db.repo.CartItemRepository;
import com.celfocus.hiring.kickstarter.db.repo.CartRepository;
import com.celfocus.hiring.kickstarter.db.repo.ProductRepository;
import com.celfocus.hiring.kickstarter.domain.Cart;
import com.celfocus.hiring.kickstarter.domain.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    @Autowired
    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    public void addItemToCart(String username, CartItemInput itemInput) {
        var cart = cartRepository.findByUserId(username).orElseGet(() -> {
            var newCart = new CartEntity();
            newCart.setUserId(username);
            return cartRepository.save(newCart);
        });

        cartItemRepository.findById(new CartItemPK(itemInput.itemId(), cart.getId()))
                .ifPresentOrElse((item) -> updateItemQuantity(item, 1), () -> {
                    addNewItemToCart(itemInput, cart);
                });
    }

    private void addNewItemToCart(CartItemInput itemInput, CartEntity cart) {
        var product = productService.reserve(itemInput.itemId(), 1);
        var cartItem = new CartItemEntity();
        cartItem.setQuantity(1);
        cartItem.setItemId(itemInput.itemId());
        cartItem.setCartId(cart.getId());
        cartItem.setCart(cart);
        cartItem.setPrice(product.getPrice());
        cartItemRepository.save(cartItem);
    }

    private void updateItemQuantity(CartItemEntity item, int byCount) {
        productService.reserve(item.getItemId(), byCount);
        setItemQuantity(item, item.getQuantity() + byCount);
    }

    private void setItemQuantity(CartItemEntity item, int quantity) {
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    public void clearCart(String username) {
        cartRepository.findByUserId(username).ifPresent(cart -> {
            List<CartItemEntity> cartItems = cart.getItems();
            cartItems.forEach(cartItem -> {
                productService.release(cartItem.getItemId(), cartItem.getQuantity());
            });

        });
        cartRepository.deleteByUserId(username);
    }

    public Cart<? extends CartItem> getCart(String username) {
        return cartRepository.findByUserId(username)
                .map(this::mapToCart)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }

    public void removeItemFromCart(String username, String itemId) {
        cartRepository.findByUserId(username)
                .ifPresent(cart -> {
                    productService.release(itemId, cart.getItems().stream()
                            .filter(i -> i.getItemId().equals(itemId)).findFirst().get().getQuantity());
                    cartItemRepository.deleteById(new CartItemPK(itemId, cart.getId()));
                });
    }

    private Cart<? extends CartItem> mapToCart(CartEntity cartEntity) {
        Cart<CartItemEntity> cart = new Cart<>();
        cart.setUserId(cartEntity.getUserId());
        cart.setItems(cartEntity.getItems());
        return cart;
    }
}
