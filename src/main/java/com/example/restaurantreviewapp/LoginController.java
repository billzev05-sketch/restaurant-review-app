package com.example.restaurantreviewapp;

import com.example.restaurantreviewapp.Admin.Admin;
import com.example.restaurantreviewapp.Critic.Critic;
import com.example.restaurantreviewapp.Owner.Owner;
import com.example.restaurantreviewapp.User.User;
import com.example.restaurantreviewapp.User.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/api/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String role,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        if ("admin".equals(role)) {
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isPresent() && userOpt.get() instanceof Admin) {
                User adminUser = userOpt.get();

                // Χρήση BCrypt για ασφαλή έλεγχο
                if (BCrypt.checkpw(password, adminUser.getPassword())) {
                    session.setAttribute("userId", adminUser.getId());
                    session.setAttribute("username", adminUser.getUsername());
                    session.setAttribute("role", "admin");
                    session.setAttribute("firstName", adminUser.getFirstName());
                    session.setAttribute("lastName", adminUser.getLastName());
                    return "redirect:/dashboard.html";
                }
            }
            return "redirect:/index.html?error=invalid_credentials";
        }

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Χρήση BCrypt για ασφαλή έλεγχο
            if (BCrypt.checkpw(password, user.getPassword())) {
                if ("critic".equals(role) && user instanceof Critic) {
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("role", "critic");
                    session.setAttribute("firstName", user.getFirstName());
                    session.setAttribute("lastName", user.getLastName());
                    return "redirect:/dashboard.html";
                } else if ("owner".equals(role) && user instanceof Owner) {
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("username", user.getUsername());
                    session.setAttribute("role", "owner");
                    session.setAttribute("firstName", user.getFirstName());
                    session.setAttribute("lastName", user.getLastName());
                    return "redirect:/dashboard.html";
                }
            }
        }

        return "redirect:/index.html?error=invalid_credentials";
    }

    @PostMapping("/api/session/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate(); // Σκοτώνει οριστικά το session
        return ResponseEntity.ok(Map.of("message", "Αποσυνδεθήκατε επιτυχώς"));
    }

    @GetMapping("/api/session/user")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSessionUser(HttpSession session) {
        Map<String, Object> userDetails = new HashMap<>();

        if (session.getAttribute("userId") == null) {
            userDetails.put("loggedIn", false);
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(userDetails);
        }

        userDetails.put("loggedIn", true);
        userDetails.put("userId", session.getAttribute("userId"));
        userDetails.put("username", session.getAttribute("username"));
        userDetails.put("role", session.getAttribute("role"));
        userDetails.put("firstName", session.getAttribute("firstName"));
        userDetails.put("lastName", session.getAttribute("lastName"));

        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(userDetails);
    }
}