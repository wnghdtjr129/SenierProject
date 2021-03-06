package com.jhs.seniorProject.controller;

import com.jhs.seniorProject.argumentresolver.Login;
import com.jhs.seniorProject.argumentresolver.LoginUser;
import com.jhs.seniorProject.service.requestform.LoginForm;
import com.jhs.seniorProject.service.requestform.SignUpForm;
import com.jhs.seniorProject.controller.logic.KaKaoLogic;
import com.jhs.seniorProject.domain.exception.DuplicatedUserException;
import com.jhs.seniorProject.domain.exception.IncorrectPasswordException;
import com.jhs.seniorProject.domain.exception.NoSuchUserException;
import com.jhs.seniorProject.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

import static com.jhs.seniorProject.controller.SessionConst.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final KaKaoLogic kakaoLogic;

    @GetMapping("/signup")
    public String createSignUpForm(@ModelAttribute("signUpForm") SignUpForm signUpForm) {
        return "users/signUpForm";
    }

    @PostMapping("/signup")
    public String signUp(@Validated @ModelAttribute SignUpForm signUpForm, BindingResult bindingResult) {
        log.info("userController --> signUpForm: {}", signUpForm);

        if (bindingResult.hasErrors()) {
            log.info("bindResult = {}", bindingResult);
            return "users/signUpForm";
        }

        try {
            userService.join(signUpForm);
        } catch (DuplicatedUserException e) {
            log.error("userController.signUp error ", e);
            bindingResult.reject("duplicateUser", "중복된 회원입니다.");
            signUpForm.setBlankAllData();
            return "users/signUpForm";
        }

        if (!signUpForm.checkPassword()) {
            bindingResult.reject("NotCorrectPassword", "비밀번호가 일치하지 않습니다");
            return "users/signUpForm";
        }

        return "redirect:/";
    }

    @PostMapping("/withdrawal")
    public String withdrawal(@Login LoginUser loginUser, HttpSession session) {
        if (isLoginStatus(loginUser)) {
            try {
                userService.withdrawal(loginUser.getId());
                session.invalidate();
            } catch (NoSuchUserException e) {
                log.error("UserController.withdrawal error", e);
                //TODO return to MyPage
            }
        }
        return "redirect:/";
    }

    @GetMapping("/login")
    public String createLoginForm(@ModelAttribute("loginForm") LoginForm loginForm) {
        return "users/loginForm";
    }

    @PostMapping("/login")
    public String login(@Validated @ModelAttribute LoginForm loginForm,
                        BindingResult bindingResult,
                        HttpSession session) {
        log.info("userController --> user.id: {}, user.password: {}", loginForm.getUserId(), loginForm.getPassword());
        if (bindingResult.hasErrors()) {
            log.info("bindResult = {}", bindingResult);
            return "users/loginForm";
        }

        try {
            LoginUser loginUser = userService.login(loginForm);
            setLoginUser(session, loginUser);
        } catch (NoSuchUserException | IncorrectPasswordException e) {
            log.error("UserController.login error", e);
            bindingResult.reject("checkIdAndPassword", "아이디와 비밀번호를 확인해 주세요");
            return "users/loginForm";
        }
        String redirect = (String) session.getAttribute("redirectURL");
        String url = redirect == null ? "/": redirect;
        return "redirect:" + url;
    }

    @GetMapping("/kakao_login")
    public String kaKaoLogin(String code, HttpSession session) {
        log.info("KaKao login execute");
        kakaoLogic.getKaKaoToken(code);
        LoginUser loginUser = kakaoLogic.logIn(session);
        setLoginUser(session, loginUser);
        return "redirect:/";
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session) {
        log.info("logout execute");
        if (isKaKaoLoginUser(session)) {
            kakaoLogic.logout(session);
        } else {
            if (session != null) {
                session.invalidate();
            }
        }
        return "redirect:/";
    }

    @GetMapping("/myPage")
    public String myPage(@Login LoginUser user, Model model, Pageable pageable) throws NoSuchUserException {
        model.addAttribute("userInfo", userService.getUserInfo(user.getId(), pageable));
        return "users/mypage";
    }

    private boolean isKaKaoLoginUser(HttpSession session) {
        return session.getAttribute(KAKAO_TOKEN) != null;
    }

    private boolean isLoginStatus(LoginUser user) {
        return user != null;
    }

    private void setLoginUser(HttpSession session, LoginUser user) {
        log.info("loginUser = {}", user);
        session.setAttribute(LOGIN_USER, user);
    }
}