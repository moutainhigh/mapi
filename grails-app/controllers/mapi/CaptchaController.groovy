package mapi

import nl.captcha.Captcha
import nl.captcha.servlet.CaptchaServletUtil

class CaptchaController {
    def final WIDTH = 160
    def final HEIGHT = 50

    def index = {
        def captcha = new Captcha.Builder(WIDTH, HEIGHT).addText().addNoise().build()
        session.captcha = captcha
        CaptchaServletUtil.writeImage(response, captcha.image)
    }

    //远程验证验证码
    def imageCaptcha = {
        if (session.captcha?.isCorrect(params.captcha.toLowerCase())) {
            render("true")
        } else {
            render("false")
        }
    }
}
