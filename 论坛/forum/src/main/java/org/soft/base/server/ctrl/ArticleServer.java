package org.soft.base.server.ctrl;

import jakarta.servlet.http.HttpSession;
import org.soft.base.model.Article;
import org.soft.base.model.Like;
import org.soft.base.model.Reply;
import org.soft.base.model.Users;
import org.soft.base.server.ctrl.dao.SplitDao;
import org.soft.base.server.mapper.ArticleMapper;
import org.soft.base.server.mapper.LikeMapper;
import org.soft.base.server.mapper.ReplyMapper;
import org.soft.base.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/article")
public class ArticleServer {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    @Qualifier("splitDao")
    private SplitDao splitDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired  // 添加这个注解
    private LikeMapper likeMapper;

    @RequestMapping("/issueOP")
    public String articleIssueServer(Article article, HttpSession session, RedirectAttributes redirectAttributes) {
        Users users = (Users) session.getAttribute("users");
        if (users != null) {
            // 刷新用户状态
            Users refreshedUser = userMapper.findUserById(users.getUserId());
            if (refreshedUser != null) {
                session.setAttribute("users", refreshedUser);
            }

            // 检查用户是否被禁言
            if (refreshedUser != null && refreshedUser.isBanned()) {
                redirectAttributes.addFlashAttribute("errorMessage", "您已被禁言，无法发布文章");
                return "redirect:/article/split/1";
            }

            int userId = users.getUserId();
            article.setUserId(userId);
            boolean b = articleMapper.articleIssueMapper(article);
            if (b) {
                redirectAttributes.addFlashAttribute("successMessage", "文章发布成功！");
                return "redirect:/article/show/" + article.getArticleId();
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "文章发布失败，请重试");
                return "issue";
            }
        } else {
            return "redirect:/";
        }
    }

    @RequestMapping("/split/{current}")
    public String articleListServer(@PathVariable("current") int current, Model model, HttpSession session) {
        // 刷新登录用户状态
        Users sessionUser = (Users) session.getAttribute("users");
        if (sessionUser != null) {
            Users refreshedUser = userMapper.findUserById(sessionUser.getUserId());
            if (refreshedUser != null) {
                session.setAttribute("users", refreshedUser);
            }
        }

        int size = 3;
        int rows = articleMapper.articleActiveRowsMapper();
        int allPages = splitDao.allPages(rows, size);
        int begin = splitDao.getBegin(current, size);
        Map<String, Integer> map = new HashMap<>();
        map.put("begin", begin);
        map.put("size", size);

        List<Article> articleList = articleMapper.activeArticleListMapper(map);
        model.addAttribute("articleList", articleList);
        model.addAttribute("allPages", allPages);
        model.addAttribute("current", current);
        return "index";
    }

    @RequestMapping("/show/{aId}")
    public String articleByIdServer(
            @PathVariable("aId") int aId,
            @RequestParam(value = "page", defaultValue = "1") int current,
            Model model,
            HttpSession session) {

        // 1. 获取文章详情
        Article article = articleMapper.articleByIdMapper(aId);
        if (article == null) {
            return "redirect:/article/split/1";
        }
        model.addAttribute("article", article);

        // 2. 获取点赞总数
        int likeCount = likeMapper.countLikesByArticle(aId);
        model.addAttribute("likeCount", likeCount);

        // 3. 获取当前用户点赞状态
        Users user = (Users) session.getAttribute("users");
        boolean isLiked = false;

        if (user != null) {
            // 使用新方法检查点赞状态
            isLiked = likeMapper.isLikedByUser(aId, user.getUserId()) > 0;
        }

        model.addAttribute("isLiked", isLiked);

        // 4. 设置分页参数
        int size = 3;
        int rows = replyMapper.repliesAllRows(aId);
        int allPages = splitDao.allPages(rows, size);
        int begin = splitDao.getBegin(current, size);

        // 5. 获取当前页的回复列表
        Map<String, Integer> map = new HashMap<>();
        map.put("begin", begin);
        map.put("size", size);
        map.put("articleId", aId);
        List<Reply> replies = replyMapper.repliesByArticleListMapper(map);

        // 6. 添加模型属性
        model.addAttribute("replies", replies);
        model.addAttribute("allPages", allPages);
        model.addAttribute("current", current);

        return "show";
    }

    @RequestMapping("/delete/{aId}")
    public String articleDeleteByIdServer(@PathVariable("aId") int aId, HttpSession session, RedirectAttributes redirectAttributes) {
        Users user = (Users) session.getAttribute("users");
        if (user == null) {
            return "redirect:/";
        }

        Article article = articleMapper.articleByIdMapper(aId);
        if (article != null && article.getUserId() == user.getUserId()) {
            boolean b = articleMapper.articleDeleteById(aId);
            if (b) {
                redirectAttributes.addFlashAttribute("successMessage", "文章删除成功！");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "文章删除失败");
            }
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "您没有权限删除此文章");
        }
        return "redirect:/article/split/1";
    }

    @RequestMapping("/articleUpdate/{aId}")
    public String toUpdatePageServer(@PathVariable("aId") int aId, Model model) {
        Article article = articleMapper.articleByIdMapper(aId);
        model.addAttribute("article", article);
        return "articleUpdate";
    }

    @RequestMapping("/update")
    public String articleUpdateServer(Article article, RedirectAttributes redirectAttributes) {
        boolean b = articleMapper.articleUpdateMapper(article);
        if (b) {
            redirectAttributes.addFlashAttribute("successMessage", "文章修改成功！");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "文章修改失败");
        }
        return "redirect:/article/show/" + article.getArticleId();
    }

    @RequestMapping("/myArticles/{current}")
    public String myArticles(
            @PathVariable("current") int current,
            HttpSession session,
            Model model) {

        Users user = (Users) session.getAttribute("users");
        if (user == null) {
            return "redirect:/";
        }

        int size = 5;
        int userId = user.getUserId();

        // 获取用户所有文章数量
        int rows = articleMapper.countArticlesByUser(userId);
        int allPages = splitDao.allPages(rows, size);
        int begin = splitDao.getBegin(current, size);

        // 获取用户所有文章
        List<Article> myArticles = articleMapper.getArticlesByUser(userId, begin, size);

        model.addAttribute("articleList", myArticles);
        model.addAttribute("allPages", allPages);
        model.addAttribute("current", current);
        return "myArticles";
    }

    @RequestMapping("/search")
    public String searchArticles(@RequestParam("keyword") String keyword, Model model) {
        List<Article> searchResults = articleMapper.searchArticlesByTitle(keyword);
        model.addAttribute("articleList", searchResults);
        model.addAttribute("searchKeyword", keyword);
        return "searchResults";
    }

    @RequestMapping("/like/{aId}")
    public String likeArticle(@PathVariable("aId") int aId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Users user = (Users) session.getAttribute("users");
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "请登录后再点赞");
            return "redirect:/article/show/" + aId;
        }

        int userId = user.getUserId();

        // 1. 检查当前点赞状态
        boolean isLiked = likeMapper.isLikedByUser(aId, userId) > 0;

        // 2. 根据状态执行操作
        if (isLiked) {
            // 取消点赞
            int deleted = likeMapper.removeLike(new Like(0, aId, userId, null));
            if (deleted > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "已取消点赞");
            } else {
                // 添加详细错误信息
                redirectAttributes.addFlashAttribute("errorMessage",
                        "取消点赞失败：数据库操作未影响任何行");
            }
        } else {
            // 添加点赞
            Like newLike = new Like(0, aId, userId, null);
            int added = likeMapper.addLike(newLike);
            if (added > 0) {
                redirectAttributes.addFlashAttribute("successMessage", "点赞成功");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "点赞失败");
            }
        }

        // 3. 强制刷新页面
        return "redirect:/article/show/" + aId + "?t=" + System.currentTimeMillis();
    }
}