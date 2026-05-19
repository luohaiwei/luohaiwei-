package org.soft.base.server.ctrl;

import jakarta.servlet.http.HttpSession;
import org.soft.base.model.Reply;
import org.soft.base.model.Users;
import org.soft.base.server.mapper.ReplyMapper;
import org.soft.base.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/reply")
public class ReplyServer {

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private UserMapper userMapper;

    @RequestMapping("/replyIssue")
    public String replyIssueServer(Reply reply, HttpSession session, RedirectAttributes redirectAttributes) {
        Users u = (Users) session.getAttribute("users");
        if (u != null) {
            Users user = userMapper.findUserById(u.getUserId());
            if (user != null && user.isBanned()) {
                redirectAttributes.addFlashAttribute("errorMessage", "您已被禁言，无法回复");
                return "redirect:/article/split/1";
            }

            boolean b = replyMapper.replyIssueMapper(reply);
            int articleId = reply.getArticleId();
            if (b) {
                int userId = u.getUserId();
                int replyId = reply.getReplyId();
                Map<String, Integer> map = new HashMap<>();
                map.put("userId", userId);
                map.put("articleId", articleId);
                map.put("replyId", replyId);
                b = replyMapper.replyRelationMapper(map);
            }

            if (b) {
                redirectAttributes.addFlashAttribute("successMessage", "回复发布成功！");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "回复发布失败");
            }
            return "redirect:/article/show/" + articleId;
        }
        return "redirect:/";
    }
}