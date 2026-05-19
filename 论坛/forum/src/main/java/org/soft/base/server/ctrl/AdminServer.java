package org.soft.base.server.ctrl;

import jakarta.servlet.http.HttpSession;
import org.soft.base.model.Admin;
import org.soft.base.model.Article;
import org.soft.base.model.Users;
import org.soft.base.server.ctrl.dao.SplitDao;
import org.soft.base.server.mapper.AdminMapper;
import org.soft.base.server.mapper.ArticleMapper;
import org.soft.base.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminServer {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    @Qualifier("splitDao")
    private SplitDao splitDao;

    @GetMapping("/login")
    public String showAdminLoginForm() {
        return "adminLogin";
    }

    @PostMapping("/login")
    public String adminLogin(Admin admin, HttpSession session, RedirectAttributes redirectAttributes) {
        Admin a = adminMapper.adminLoginMapper(admin);
        if (a != null) {
            session.setAttribute("admin", a);
            redirectAttributes.addFlashAttribute("successMessage", "管理员登录成功！");
            return "redirect:/admin/userList/1";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "管理员账号或密码错误");
            return "redirect:/admin/login";
        }
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("admin");
        redirectAttributes.addFlashAttribute("successMessage", "您已成功退出管理员登录");
        return "redirect:/admin/login";
    }

    @RequestMapping("/userList/{current}")
    public String userList(@PathVariable("current") int current, Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        int size = 3;
        int rows = userMapper.userAllRowsMapper();
        int allPages = splitDao.allPages(rows, size);
        int begin = splitDao.getBegin(current, size);
        Map<String, Integer> map = new HashMap<>();
        map.put("begin", begin);
        map.put("size", size);

        List<Users> userList = userMapper.userListMapper(map);
        model.addAttribute("userList", userList);
        model.addAttribute("allPages", allPages);
        model.addAttribute("current", current);
        return "adminUserList";
    }

    @RequestMapping("/userDetail/{userId}")
    public String userDetail(@PathVariable("userId") int userId, Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        Users user = userMapper.findUserById(userId);
        model.addAttribute("user", user);
        return "adminUserDetail";
    }

    @RequestMapping("/deleteUser/{userId}")
    public String deleteUser(
            @PathVariable("userId") int userId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        // 删除用户
        userMapper.deleteUser(userId);

        redirectAttributes.addFlashAttribute("successMessage", "用户删除成功");
        return "redirect:/admin/userList/1";
    }

    @RequestMapping("/banUser/{userId}")
    public String banUser(@PathVariable("userId") int userId, HttpSession session, RedirectAttributes redirectAttributes) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        userMapper.updateUserBanStatus(userId, true);
        redirectAttributes.addFlashAttribute("successMessage", "用户已禁言");
        return "redirect:/admin/userList/1";
    }

    @RequestMapping("/unbanUser/{userId}")
    public String unbanUser(@PathVariable("userId") int userId, HttpSession session, RedirectAttributes redirectAttributes) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        userMapper.updateUserBanStatus(userId, false);
        redirectAttributes.addFlashAttribute("successMessage", "用户已解除禁言");
        return "redirect:/admin/userList/1";
    }

    @RequestMapping("/articleList/{current}")
    public String articleList(@PathVariable("current") int current, Model model, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        int size = 5;
        int rows = articleMapper.articleAllRowsMapper();
        int allPages = splitDao.allPages(rows, size);
        int begin = splitDao.getBegin(current, size);

        Map<String, Integer> map = new HashMap<>();
        map.put("begin", begin);
        map.put("size", size);

        List<Article> articleList = articleMapper.articleListMapper(map);
        model.addAttribute("articleList", articleList);
        model.addAttribute("allPages", allPages);
        model.addAttribute("current", current);
        return "adminArticleList";
    }

    @RequestMapping("/putOnSale/{aId}")
    public String putOnSale(@PathVariable("aId") int aId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        boolean success = articleMapper.articlePutOnSale(aId);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "文章已上架");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "文章上架失败");
        }
        return "redirect:/admin/articleList/1";
    }

    @RequestMapping("/takeOffSale/{aId}")
    public String takeOffSale(@PathVariable("aId") int aId, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("admin") == null) {
            return "redirect:/admin/login";
        }
        boolean success = articleMapper.articleTakeOffSale(aId);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "文章已下架");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "文章下架失败");
        }
        return "redirect:/admin/articleList/1";
    }

    @RequestMapping("/articleDetail/{aId}")
    public String adminArticleDetail(
            @PathVariable("aId") int aId,
            Model model,
            HttpSession session) {

        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        Article article = articleMapper.articleByIdMapper(aId);
        if (article == null) {
            return "redirect:/admin/articleList/1";
        }

        model.addAttribute("article", article);
        return "adminArticleDetail";
    }
}