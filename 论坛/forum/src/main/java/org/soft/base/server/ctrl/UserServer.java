package org.soft.base.server.ctrl;

import jakarta.servlet.http.HttpSession;
import org.soft.base.model.Users;
import org.soft.base.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
@SessionAttributes({"users"})
public class UserServer {

    @Autowired
    private UserMapper userMapper;

    @RequestMapping("/registerServer")
    public String userRegisterServer(Users users, Model model, RedirectAttributes redirectAttributes) {
        // 检查用户名是否重复
        Users existingUser = userMapper.findUserByUserName(users.getUserName());
        if (existingUser != null) {
            model.addAttribute("errorMessage", "用户名已存在，请选择其他用户名。");
            return "register";
        }

        // 修改：使用计数方法检查邮箱重复
        int emailCount = userMapper.countByEmail(users.getUserEmail());
        if (emailCount > 0) {
            model.addAttribute("errorMessage", "邮箱已被注册，请使用其他邮箱。");
            return "register";
        }

        boolean b = userMapper.userRegisterMapper(users);
        if (b) {
            redirectAttributes.addFlashAttribute("successMessage", "注册成功！请登录您的账户。");
            return "redirect:/";
        } else {
            model.addAttribute("errorMessage", "注册失败，请重试");
            return "register";
        }
    }

    @RequestMapping("/loginServer")
    public String userLoginServer(Users users, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        Users u = userMapper.userLoginMapper(users);
        if (u != null) {
            model.addAttribute("users", u);
            session.removeAttribute("loginFailCount");
            redirectAttributes.addFlashAttribute("successMessage", "登录成功！欢迎回来，" + u.getUserName());
            return "redirect:/article/split/1";
        } else {
            Integer failCount = (Integer) session.getAttribute("loginFailCount");
            if (failCount == null) {
                failCount = 1;
            } else {
                failCount++;
            }
            session.setAttribute("loginFailCount", failCount);
            model.addAttribute("errorMessage", "用户名或密码错误");
            return "login";
        }
    }

    @RequestMapping("/updateServer")
    public String userUpdateServer(Users users, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Users u = (Users) session.getAttribute("users");
        if (u != null) {
            Users existingUser = userMapper.findUserByUserName(users.getUserName());
            if (existingUser != null && existingUser.getUserId() != u.getUserId()) {
                model.addAttribute("updateMessage", "用户名已被使用，请选择其他用户名");
                return "update";
            }

            users.setUserId(u.getUserId());
            boolean b = userMapper.userUpdateServer(users);
            if (b) {
                u.setUserName(users.getUserName());
                u.setUserPassword(users.getUserPassword());
                redirectAttributes.addFlashAttribute("successMessage", "个人信息更新成功！");
                return "redirect:/article/split/1";
            } else {
                model.addAttribute("updateMessage", "修改失败，请重试");
                return "update";
            }
        } else {
            return "redirect:/";
        }
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        Users user = (Users) session.getAttribute("users");
        if (user != null) {
            session.removeAttribute("users");
            redirectAttributes.addFlashAttribute("successMessage", "您已成功退出登录");
        }
        return "redirect:/";
    }

    @RequestMapping("/refreshSession")
    public String refreshUserSession(HttpSession session, RedirectAttributes redirectAttributes) {
        Users sessionUser = (Users) session.getAttribute("users");
        if (sessionUser != null) {
            Users currentUser = userMapper.findUserById(sessionUser.getUserId());
            if (currentUser != null) {
                session.setAttribute("users", currentUser);
                redirectAttributes.addFlashAttribute("successMessage", "用户信息已刷新");
            }
        }
        return "redirect:/article/split/1";
    }
}