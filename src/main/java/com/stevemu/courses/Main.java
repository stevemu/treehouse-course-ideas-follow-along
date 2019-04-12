package com.stevemu.courses;

import com.stevemu.courses.model.CourseIdea;
import com.stevemu.courses.model.CourseIdeaDAO;
import com.stevemu.courses.model.NotFoundException;
import com.stevemu.courses.model.SmpleCourseIdeaDAO;
import spark.ModelAndView;
import spark.Request;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main {
    private static final String FLASH_MESSAGE_KEY = "flash_message";

    public static void main(String[] args) {

        staticFileLocation("/public");
        CourseIdeaDAO dao = new SmpleCourseIdeaDAO();


        before((req, res) -> {
            if (req.cookie("username") != null) {
                req.attribute("username", req.cookie("username"));
            }

            // create an model with flashMessage in it
            Map<String, Object> model = new HashMap<>();
            if (req.session().attributes().contains(FLASH_MESSAGE_KEY)) {
                model.put("flashMessage", captureFlashMessage(req));
            }
            req.attribute("model", model);
        });

        before("/ideas", (req, res) -> {
            if (req.attribute("username") == null) {
                setFlashMessage(req, "Whoops, please sign in first!");
                res.redirect("/");
                halt();
            }
        });

        get("/", (req, res) -> {
            Map<String, String> model = req.attribute("model");
            model.put("username", req.attribute("username"));
            return new ModelAndView(model, "index.hbs");
        }, new HandlebarsTemplateEngine());

        post("/sign-in", (req, res) -> {
            String username = req.queryParams("username");
            res.cookie("username", username);
            res.redirect("/");
            return null;
        });

        get("/ideas", (req, res) -> {
            Map<String, Object> model = req.attribute("model");
            model.put("ideas", dao.findAll());
            return new ModelAndView(model, "ideas.hbs");
        }, new HandlebarsTemplateEngine());

//        post request get pattern - PRG
        post("/ideas", (req, res) -> {
            String title = req.queryParams("title");
            CourseIdea courseIdea = new CourseIdea(title, req.attribute("username"));
            dao.add(courseIdea);
            res.redirect("/ideas");
            return null;
        });

        post("/ideas/:slug/vote", (req, res) -> {
            CourseIdea idea = dao.findBySlug(req.params("slug"));
            boolean added = idea.addVoter(req.attribute("username"));
            if (added) {
                setFlashMessage(req, "Thanks for your vote!");
            } else {
                setFlashMessage(req, "You already voted");
            }
            res.redirect("/ideas");
            return null;
        });

        get("/ideas/:slug", (req, res) -> {
            CourseIdea idea = dao.findBySlug(req.params("slug"));
            Map<String, Object> model = req.attribute("model");
            model.put("idea", idea);
            return new ModelAndView(model, "idea.hbs");
        }, new HandlebarsTemplateEngine());

        after((req, res) -> {

        });

        exception(NotFoundException.class, (exc, req, res) -> {
            res.status(404);
            HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
            String html = engine.render(new ModelAndView(null, "not-found.hbs"));
            res.body(html);
        });


    }

    private static void setFlashMessage(Request req, String message) {
        req.session().attribute(FLASH_MESSAGE_KEY, message);
    }

    private static String getFlashMessage(Request req) {
//        System.out.println(req.session().attributes().toString());
        if (req.session(false) == null) {
            return null;
        }

        if (!req.session().attributes().contains(FLASH_MESSAGE_KEY)) {
            return null;
        }
        return (String) req.session().attribute(FLASH_MESSAGE_KEY);
    }

    private static String captureFlashMessage(Request req) {
        String message = getFlashMessage(req);
        if (message != null) {
            req.session().removeAttribute(FLASH_MESSAGE_KEY);
        }
        return message;
    }


}

/*
 *
 * server-client
 * client send request with a verb and data, server can reply with json or html
 *
 * */