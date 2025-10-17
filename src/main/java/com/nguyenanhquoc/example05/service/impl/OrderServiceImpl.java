package com.nguyenanhquoc.example05.service.impl;


import com.nguyenanhquoc.example05.entity.*;
import com.nguyenanhquoc.example05.exceptions.APIException;
import com.nguyenanhquoc.example05.exceptions.ResourceNotFoundException;
import com.nguyenanhquoc.example05.payloads.dto.OrderDTO;
import com.nguyenanhquoc.example05.payloads.dto.OrderItemDTO;
import com.nguyenanhquoc.example05.payloads.response.OrderResponse;
import com.nguyenanhquoc.example05.repository.CartRepo;
import com.nguyenanhquoc.example05.repository.OrderItemRepo;
import com.nguyenanhquoc.example05.repository.OrderRepo;
import com.nguyenanhquoc.example05.repository.PaymentRepo;
import com.nguyenanhquoc.example05.service.CartService;
import com.nguyenanhquoc.example05.service.OrderService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderRepo orderRepo;


    @Autowired
    private CartRepo cartRepo;


    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private OrderItemRepo orderItemRepo;

    // Từ image_3eae5b.png
    @Autowired
    private CartService cartService;

    // Từ image_3eae5b.png
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public OrderDTO placeOrder(String emailId, Long cartId, String paymentMethod) {
        // 1. Lấy Cart bằng email và cartId
        Cart cart = cartRepo.findCartByEmailAndCartId(emailId, cartId);

        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }

        // 2. Tạo Order Entity và thiết lập các trường ban đầu
        Order order = new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted!");

        // 3. Tạo Payment Entity và thiết lập mối quan hệ OneToOne
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);

        // Lưu Payment (PaymentRepo.save)
        payment = paymentRepo.save(payment);

        // Liên kết Payment với Order
        order.setPayment(payment);

        // Lưu Order (OrderRepo.save)
        Order savedOrder = orderRepo.save(order);

        // 4. Lấy CartItems và kiểm tra giỏ hàng trống
        List<CartItem> cartItems = cart.getCartItems();

        if (cartItems.size() == 0) {
            throw new APIException("Cart is empty");
        }

        // 5. Tạo OrderItems từ CartItems
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();

            // Chuyển dữ liệu từ CartItem sang OrderItem
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());

            // Liên kết OrderItem với Order đã lưu
            orderItem.setOrder(savedOrder);

            orderItems.add(orderItem);
        }

        // Lưu tất cả OrderItems
        orderItems = orderItemRepo.saveAll(orderItems);

        // 6. Xử lý CartItems: xóa khỏi giỏ và cập nhật tồn kho (đã được trừ)
        cart.getCartItems().forEach(item -> {
            int quantity = item.getQuantity();
            Product product = item.getProduct();

            // Xóa item khỏi cart (CartService.deleteProductFromCart)
            cartService.deleteProductFromCart(cartId, item.getProduct().getProductId());

            // Cập nhật tồn kho (Logic này hơi lạ vì tồn kho đã được trừ ở addProductToCart,
            // nhưng đoạn code hiển thị rõ ràng là trừ quantity lần nữa - có thể là lỗi logic
            // hoặc logic phức tạp hơn. Ta chỉ trích xuất như code hiển thị.)
            product.setQuantity(product.getQuantity() - quantity);
        });

        // 7. Chuyển đổi sang OrderDTO và trả về
        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

        // Thêm danh sách OrderItemDTO vào OrderDTO
        orderItems.forEach(item -> orderDTO.getOrderItems().add(modelMapper.map(item, OrderItemDTO.class)));

        return orderDTO;
    }

    @Override
    public OrderDTO getOrder(String emailId, Long orderId) {
        // 1. Tìm Order Entity theo email và orderId
        Order order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);

        // 2. Kiểm tra nếu không tìm thấy
        if (order == null) {
            // Ném ngoại lệ ResourceNotFoundException
            throw new ResourceNotFoundException("Order", "orderId", orderId);
        }

        // 3. Chuyển đổi Order Entity sang OrderDTO và trả về
        return modelMapper.map(order, OrderDTO.class);
    }

    @Override
    public List<OrderDTO> getOrdersByUser(String emailId) {
        // 1. Tìm tất cả các Order Entity theo email
        List<Order> orders = orderRepo.findAllByEmail(emailId);

        // 2. Chuyển đổi danh sách Order Entities sang List<OrderDTO>
        List<OrderDTO> orderDTOs = orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .collect(Collectors.toList());

        // 3. Kiểm tra nếu danh sách trống
        if (orderDTOs.size() == 0) {
            // Ném ngoại lệ APIException
            throw new APIException("No orders placed yet by user with email: " + emailId);
        }

        // 4. Trả về List<OrderDTO>
        return orderDTOs;
    }

    @Override
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        // 1. Tạo đối tượng Sort cho phân trang
        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // 2. Tạo đối tượng Pageable
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sort);

        // 3. Thực hiện truy vấn phân trang
        Page<Order> pageOrders = orderRepo.findAll(pageDetails);

        // 4. Lấy nội dung (List<Order>)
        List<Order> orders = pageOrders.getContent();

        // 5. Chuyển đổi List<Order> sang List<OrderDTO>
        List<OrderDTO> orderDTOs = orders.stream()
                .map(order -> modelMapper.map(order, OrderDTO.class))
                .collect(Collectors.toList());

        // 6. Kiểm tra nếu danh sách trống
        if (orderDTOs.size() == 0) {
            throw new APIException("No orders placed yet by the users");
        }

        // 7. Thiết lập đối tượng OrderResponse
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setContent(orderDTOs);
        orderResponse.setPageNumber(pageOrders.getNumber());
        orderResponse.setPageSize(pageOrders.getSize());
        orderResponse.setTotalElements(pageOrders.getTotalElements());
        orderResponse.setTotalPages(pageOrders.getTotalPages());
        orderResponse.setLastPage(pageOrders.isLast());

        // 8. Trả về OrderResponse
        return orderResponse;
    }

    @Override
    public OrderDTO updateOrder(String emailId, Long orderId, String orderStatus) {
        // 1. Tìm Order Entity theo email và orderId
        Order order = orderRepo.findOrderByEmailAndOrderId(emailId, orderId);

        // 2. Kiểm tra nếu không tìm thấy
        if (order == null) {
            // Ném ngoại lệ ResourceNotFoundException
            throw new ResourceNotFoundException("Order", "orderId", orderId);
        }

        // 3. Cập nhật trạng thái đơn hàng
        order.setOrderStatus(orderStatus);
        // Giả định orderRepo.save(order) được gọi ở đây hoặc trong transaction.

        // 4. Chuyển đổi Order Entity đã cập nhật sang OrderDTO và trả về
        return modelMapper.map(order, OrderDTO.class);
    }
}
