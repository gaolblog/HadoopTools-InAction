#!/bin/bash
#This is root's the eleventh shell program.
# 判断用户输入，打印列表选项
# date:2017年 05月 28日 星期日 06:03:02 JST
# author:root 
    
# 列表选项
echo 'If you want to go to Beijing,enter 1.'
echo 'If you want to go to shanghai,enter 2.'
echo 'If you want to go to Nanjing,enter 3.'
echo 'If you want to go to Tibet,enter 4.'    
read -t 30 -p "Please input your choice: " cho
    
case $cho in
    "1")
       echo "Your Beijing ticket has been prepared for you!"
       ;;
    "2")
       echo "Your shanghai ticket has been prepared for you!"
       ;;
    "3")    
       echo "Your Nanjing ticket has been prepared for you!"
       ;;
    "4")
       echo "Your Tibet ticket has been prepared for you!"
       ;;
      *)
       echo "Error!You can only enter 1/2/3/4."
esac        
