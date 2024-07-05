package gift.wishlist;

import gift.auth.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/wishlist")
public class WishListController {

    private static final Logger logger = Logger.getLogger(WishListController.class.getName());
    private final WishListService wishListService;
    private final JwtUtil jwtUtil;

    public WishListController(WishListService wishListService, JwtUtil jwtUtil) {
        this.wishListService = wishListService;
        this.jwtUtil = jwtUtil;
    }

    private String extractEmailFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warning("Unauthorized access attempt without Bearer token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String token = authHeader.substring(7);
        String tokenEmail;
        try {
            tokenEmail = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            logger.warning("Failed to extract email from token: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return tokenEmail;
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<WishListDTO>> getWishList(@PathVariable("email") String email,
        HttpServletRequest request) {
        logger.info("getWishList called with email: " + email);

        String tokenEmail = extractEmailFromToken(request);
        if (!email.equals(tokenEmail)) {
            logger.warning("Forbidden access attempt with mismatched email: " + email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Forbidden
        }

        List<WishListDTO> wishLists = wishListService.getWishListsByEmail(email);
        logger.info("Successfully retrieved wishlist for email: " + email);
        return ResponseEntity.ok(wishLists);
    }

    @PostMapping("/{email}")
    public ResponseEntity<String> addWishList(@PathVariable("email") String email,
        HttpServletRequest request, @RequestBody WishListDTO wishListDTO) {
        logger.info("addWishList called with email: " + email);

        String tokenEmail = extractEmailFromToken(request);

        if (!email.equals(tokenEmail)) {
            logger.warning("Forbidden access attempt with mismatched email: " + email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("잘못된 토큰 접근입니다."); // Forbidden
        }
        wishListDTO.setEmail(email);
        wishListService.addWishList(wishListDTO);
        logger.info("Successfully added wishlist for email: " + email);
        return ResponseEntity.status(HttpStatus.CREATED).body("위시리스트에 추가되었습니다.");
    }

    @PutMapping("/{email}/{name}")
    public ResponseEntity<String> updateWishList(@PathVariable("email") String email,
        @PathVariable("name") String name, HttpServletRequest request,
        @RequestBody WishListDTO wishListDTO) {

        logger.info("updateWishList called with email: " + email + " and name: " + name);

        String tokenEmail = extractEmailFromToken(request);

        if (!email.equals(tokenEmail)) {
            logger.warning("Forbidden access attempt with mismatched email: " + email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("잘못된 토큰 접근입니다."); // Forbidden
        }

        WishList wishList = wishListService.getWishListByEmailAndName(email,name);
        if (wishList != null) {
            wishListService.updateWishList(email, name, wishListDTO.getNum());
            logger.info("Successfully updated wishlist for email: " + email + " and name: " + name);
            return ResponseEntity.ok().body("업데이트 성공!");
        }

        logger.warning("Wishlist not found for email: " + email + " and name: " + name);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("잘못된 토큰 접근입니다.");
    }

    @DeleteMapping("/{email}/{name}")
    public ResponseEntity<String> deleteWishList(@PathVariable("email") String email,
        @PathVariable("name") String name, HttpServletRequest request) {
        logger.info("deleteWishList called with email: " + email + " and name: " + name);

        String tokenEmail = extractEmailFromToken(request);

        if (!email.equals(tokenEmail)) {
            logger.warning("Forbidden access attempt with mismatched email: " + email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("잘못된 토큰 접근입니다."); // Forbidden
        }

        boolean deleted = wishListService.deleteWishList(email, name);
        if (deleted) {
            logger.info("Successfully deleted wishlist for email: " + email + " and name: " + name);
            return ResponseEntity.ok().body("삭제되었습니다.");
        }

        logger.warning("Wishlist not found for email: " + email + " and name: " + name);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("잘못된 토큰 접근입니다.");
    }
}
