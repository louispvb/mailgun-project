(ns mailgun-project.templates.core
  ; I was almost about to make my own templating library
  ; using regexes until I found clostache.
  (:require [clostache.parser :as stache]))


; These templates would ideally be stored in a memory backed database like Redis, as their access is
; very frequent and the size of all templates is low.
(def all-templates
  {"reset"
   "<html>
      <body>
        <h1>So you forgot your password huh?</h1>
        <h3>No problem, {{user}}.</h3>
        <p>As long as you can keep resetting, there's no worry!</p>
        <p>Reset your password every time you login!</p>
        <p>Just to make it worse your new password is a bunch of random alphabets and numbers, here it is: {{new-password}}</p>
        <p>But you might want to seriously think about making a password you might remember so <a href=\"{{new-password-link}}\">click here</a> to set your own.</p>
      </body>
    </html>"

   "welcome"
   "<html>
      <body>
        <h1>Welcome to {{company}}, {{user}}.</h1>
        <h3>We built an awesome product. A great product. The best.</h3>
        <a href=\"{{link}}\">Get started now.</a>
      </body>
    </html>"})

(defn render-template
  "Takes a name to render a predefined template with a context map containing keys and values to insert into the template"
  [name context]
  (stache/render (get all-templates name) context))
