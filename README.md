# Tame-The-Fox
 
# How does the mod work?
- In this mod, the CustomFox.java class is created which completely copies the original fox.java with some changes. The most important thing that has been changed and what makes the fox tamable is that it was initially fox is extending class Animal, so now it exends TamableAnimal. In other words, I am creating a completely independent entity so that in the future it will be easier to make changes to the code and I will not have to resort to mixins. Actually, my code may be dumb because... it was not originally intended for anyone except me
