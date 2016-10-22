# **HUE B SMART**
Hue B Smart - total control of Hue Bulbs, Groups, Scenes, and more!


This is a follow-up to my earlier Hue Connect Service Manager called Hue Lights Groups Scenes (oh my).  This version has ability to integrate & control all of your (i) Hue Color Bulbs, (ii) Hue Lux Bulbs, (iii) Hue Color Groups, (iv) Hue Lux-Only Groups, and (v) Hue Scenes.

Its very fast and responsive (whether controlling a bulb, group, or a scene).  It is a complete overhaul from the HLGS app, so if you are switching from that app, you will unfortunately need to start over.  A PITA, yes, but I'm sure that you'll be very happy with the end result.

**Beta**
This is a "beta" app only because I haven't finished with some additional functionality.  I'm releasing this version because recently there seems to be a lot of people having trouble with older Hue Connect Service Managers.  

To install, you will need to import the Hue B Smart app *AND* all of the relevant DTHs into the IDE.  If you only have Hue Color bulbs, then you will need to import (i) the bridge DTH, (ii) the Hue Bulb DTH, (iii) the Hue Group DTH, (iv) and the Hue Scenes DTH.  If you also have Hue Lux bulbs, then you will also need to import (v) the Hue Lux Bulb DTH and (vi) the Hue Lux Group DTH.

This version does not have the ability to create, delete, or modify groups or the ability to create or delete scenes directly.  That functionality will come in version 1.1, along with the ability to incorporate Hue Hub schedules (and maybe more!).  In the meantime, if you need to create / delete groups or scenes, you will need to use either the Philips Hue app or the CLIP API debugger (instructions for the CLIP debugger can be found [here](http://www.developers.meethue.com/documentation/getting-started).  

You _can_ update the level, hue, and saturation settings for an existing scene by clicking the "Update Scene" tile in any Scene device.  Note, however, that all of the lights of that scene will be updated when you do so.

**Scenes**
As in the HLGS app, scenes are momentary devices.  A new feature is the "setToGroup" function for scenes.  This function (which you can use in CoRE) allows you to apply the selected scene to a single Group -- rather than to all of the bulbs in that Scene.  This means that you do not need to set up individual scenes for each Room / Group, thus drastically reducing the number of scenes you need.  

For example, I have 25 Hue lights in my house.  I set up a single scene for each of my location modes (e.g., "Morning", "Daytime", "Nighttime", "Late", etc.).  I then set up a CoRE Piston for each room of my house, and when the appropriate conditions are met for each room, I use the same Scene momentary device as the action, calling the "setToGroup" function and use that room's group number as the parameter.  My Living Room group has the groupID of "1" (each  Group device displays its groupID number), so to apply the Morning Scene only in my Living Room, I select the "Morning" momentary switch, use the "setToGroup" task, and set Parameter #1 (type Number) to the value of 1.  I then limit that action to execute only when my house is in Morning mode, and repeat this for each of my modes.  

In this way, I really don't use the groups themselves at all -- they are just parameters for how / where I want my Scenes to work.  

Of course, if I want any scene to work on all of the lights, I just push that Scene's momentary button (scenes, by default, use a Group of "0" - which the Hue Hub interprets as "all lights").

**Chatty IDE**
One final note - because I'm still working on a few things, the logs for this version will be very "verbose".  I'll remove most of the log information in the next version.


Please let me know if you run into any problems, or have any suggestions.  
