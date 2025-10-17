package com.nguyenanhquoc.example05.service.impl;

import com.nguyenanhquoc.example05.entity.Cart;
import com.nguyenanhquoc.example05.entity.CartItem;
import com.nguyenanhquoc.example05.entity.Product;
import com.nguyenanhquoc.example05.entity.User;
import com.nguyenanhquoc.example05.exceptions.APIException;
import com.nguyenanhquoc.example05.exceptions.ResourceNotFoundException;
import com.nguyenanhquoc.example05.payloads.dto.CartDTO;
import com.nguyenanhquoc.example05.payloads.dto.ProductDTO;
import com.nguyenanhquoc.example05.repository.CartItemRepo;
import com.nguyenanhquoc.example05.repository.CartRepo;
import com.nguyenanhquoc.example05.repository.ProductRepo;
import com.nguyenanhquoc.example05.repository.UserRepo;
import com.nguyenanhquoc.example05.service.CartService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CartDTO addProductToCart(String email, Long productId, Integer quantity) {

        // 1. Lấy user theo email
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // 2. Lấy giỏ hàng của user hoặc tạo mới nếu chưa có
        Cart cart = cartRepo.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart.setTotalPrice(0.0);
            return cartRepo.save(newCart);
        });

        // 3. Lấy sản phẩm
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        // 4. Kiểm tra sản phẩm đã có trong giỏ hay chưa
        CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null) {
            // Cộng dồn quantity
            int newQuantity = cartItem.getQuantity() + quantity;

            if (product.getQuantity() < quantity) {
                throw new APIException("Not enough stock for " + product.getProductName());
            }

            cartItem.setQuantity(newQuantity);
            cartItem.setProductPrice(product.getSpecialPrice()); // cập nhật giá nếu cần
            cartItemRepo.save(cartItem);

        } else {
            // Thêm CartItem mới
            if (product.getQuantity() < quantity) {
                throw new APIException("Not enough stock for " + product.getProductName());
            }

            CartItem newCartItem = new CartItem();
            newCartItem.setProduct(product);
            newCartItem.setCart(cart);
            newCartItem.setQuantity(quantity);
            newCartItem.setDiscount(product.getDiscount());
            newCartItem.setProductPrice(product.getSpecialPrice());
            cartItemRepo.save(newCartItem);
        }

        // 5. Cập nhật tồn kho và tổng tiền giỏ
        product.setQuantity(product.getQuantity() - quantity);
        double totalPrice = cart.getCartItems().stream()
                .mapToDouble(ci -> ci.getProductPrice() * ci.getQuantity())
                .sum();
        cart.setTotalPrice(totalPrice);
        cartRepo.save(cart);

        // 6. Mapping sang DTO
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<ProductDTO> productDTOs = cart.getCartItems().stream()
                .map(ci -> modelMapper.map(ci.getProduct(), ProductDTO.class))
                .collect(Collectors.toList());
        cartDTO.setProducts(productDTOs);

        return cartDTO;
    }


    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepo.findAll();

        if (carts.size() == 0) {
            throw new APIException("No cart exists");
        }

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream()
                    .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class)).collect(Collectors.toList());

            cartDTO.setProducts(products);

            return cartDTO;
        }).collect(Collectors.toList());

        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);

        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class)).collect(Collectors.toList());

        cartDTO.setProducts(products);

        return cartDTO;
    }

    @Override
    public CartDTO updateProductQuantityInCart(Long cartId, Long productId, Integer quantity) {
        // 1. Lấy Cart và Product
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));


        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }


        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }


        CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }


        Integer oldQuantity = cartItem.getQuantity();


        product.setQuantity(product.getQuantity() + oldQuantity);


        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * oldQuantity));


        cartItem.setQuantity(quantity);

        product.setQuantity(product.getQuantity() - quantity);


        cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));


        cartItem = cartItemRepo.save(cartItem);

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<ProductDTO> productDTOs = cart.getCartItems().stream()
                .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class))
                .collect(Collectors.toList());

        cartDTO.setProducts(productDTOs);

        return cartDTO;
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }


        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());


        cartItem.setProductPrice(product.getSpecialPrice());


        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepo.save(cartItem);
        cartRepo.save(cart);
    }

    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        // 1. Lấy Cart
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        // 2. Lấy CartItem
        CartItem cartItem = cartItemRepo.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        // 3. Hoàn trả số lượng vào tồn kho
        Product product = cartItem.getProduct();
        product.setQuantity(product.getQuantity() + cartItem.getQuantity());

        // 4. Trừ tổng tiền của mục hàng khỏi giỏ hàng
        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));

        // 5. Xóa CartItem
        // Dùng @Modifying query để xóa: deleteCartItemByProductIdAndCartId(Long productId, Long cartId)
        cartItemRepo.deleteCartItemByProductIdAndCartId(cart.getCartId(), productId);

        // 6. Lưu lại Cart và Product (giả định được thực hiện)

        return "Product " + product.getProductName() + " removed from the cart !!!";
    }
}
