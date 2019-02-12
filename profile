VISUAL=$EDITOR
FCEDIT=$EDITOR
ENV=$HOME/.kshrc
export EDITOR ENV VISUAL FCEDIT

#==========================================================#
# Set terminal options                                     #
#==========================================================#
if [ ! "$DT" ]
then
	stty erase "^h" echo echoe echok ixon -ixany ixoff tabs

fi

# set misc vars                                            #
#==========================================================#
export TERMINFO=/usr/local/lib/terminfo
export HOST=`uname -n`
export HISTFILE=$HOME/.ksh_history
export HISTSIZE=4096
export PS1='$HOST:$PWD> '
#export PS1='$HOST:$LOGNAME> '#
export EDITOR=vi
export VISUAL=${EDITOR}
export FCEDIT=${EDITOR}
export SHELL=/usr/bin/ksh
export KSHRC=$HOME/.kshrc
#ENV='${KSHRC[(_$-=1)+(_=0)-(_$-!=_${-%%*i*})]}'
export ENV=$KSHRC

export PATH=.:$HOME/bin:\
$PATH:\
/usr/local/bin:\
/usr/sbin:\
/home/jh253k/bin

export MANPATH=$HOME/local/man:\
$MANPATH:\
/usr/local/man:\
/usr/local/gnu/man:\
/opt/tools/FSFgcc/man

m=`date +%m`; d=`date +%d` ; y=`date +%y`

alias h='tail'
alias winb='/usr/openwin/bin/xterm -bg black -fg white -cr white -ms red -sb -sl 10000 -T home -geom 70x45 &'
alias ll='ls -aCFlrt'
alias cls=clear 
alias win='xterm -cr white -ms red -sb -sl 10000 -T home -geom 70x45 &'
alias winb='xterm -bg black -fg white -cr white -ms red -sb -sl 10000 -T home -geom 70x45 &' 

stty erase ^H
