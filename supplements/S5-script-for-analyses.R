library(ggplot2)
library(gridExtra)

# updated 210128 according to reviewers' suggestions

# load the integrated dataset

ico<-read.table('S4-iconevol-combined-dataset.csv', sep=",", header=T)

# reduce to the relevant datapoints
# (only positive iconicity values larger than 1, SSt values with enough data)

ico<-ico[ico[, "St_StableShiftInGroup"] > 0,]
ico<-ico[ico[, "Icon_fit"] > 1,]

# LREG plot: Scatter plot with regression lines marginal density plots of SSt and Ico values, 
#            color-coded for vowels vs. consonants (appears as figure 3a in paper)

# scatter plot of SSt and Icon_fit values, color by groups
scatterPlot <- ggplot(ico, aes(x=St_StableShiftInGroup, y=log(Icon_fit-1), color=V.C, shape=V.C)) +
    geom_point() +
    geom_smooth(method=lm, se=FALSE, fullrange=TRUE) +
    scale_color_manual(values = c('#999999','#E69F00')) +
    theme(axis.text.y = element_text(angle = 90, hjust = 0.5)) +
    xlab('Sound_stability') + ylab('Log(iconic_value)') +
    #theme(legend.position=c(0.05,0.97), legend.justification=c(0,1)) # no log
    theme(legend.position=c(0.76,0.285), legend.justification=c(0,1)) # log

# Marginal density plot of SSt (top panel)
xdensity <- ggplot(ico, aes(x=St_StableShiftInGroup, fill=V.C)) + 
    geom_density(alpha=.5) + 
    scale_fill_manual(values = c('#999999','#E69F00')) + 
    theme(axis.text.y = element_text(angle = 90, hjust = 0.5)) +
    ylab('Density') +
    theme(axis.title.x=element_blank(),
          axis.text.x=element_blank(),
          axis.ticks.x=element_blank()) +
    theme(legend.position = "none")

# Marginal density plot of Ico (right panel)
ydensity <- ggplot(ico, aes(x=log(Icon_fit-1), fill=V.C)) + 
    geom_density(alpha=.5) + 
    scale_fill_manual(values = c('#999999','#E69F00')) + 
    theme(axis.text.y = element_text(angle = 90, hjust = 0.5)) +
    ylab('Density') +
    theme(axis.title.y=element_blank(),
          axis.text.y=element_blank(),
          axis.ticks.y=element_blank()) +
    theme(legend.position = "none") +
    coord_flip()

# put all plots together

blankPlot <- ggplot()+geom_blank(aes(1,1))+
    theme(plot.background = element_blank(), 
          panel.grid.major = element_blank(),
          panel.grid.minor = element_blank(), 
          panel.border = element_blank(),
          panel.background = element_blank(),
          axis.title.x = element_blank(),
          axis.title.y = element_blank(),
          axis.text.x = element_blank(), 
          axis.text.y = element_blank(),
          axis.ticks = element_blank()
    )

pdf("S6-result-graphs/lreg.pdf", width = 6, height = 3)
grid.arrange(xdensity, blankPlot, scatterPlot, ydensity, 
             ncol=2, nrow=2, widths=c(4, 1.4), heights=c(1.4, 4))
dev.off()

# create subsets for vowels and consonants

ico.c<-ico[!is.na(ico$C_PoA) | !is.na(ico$C_MoA) | !is.na(ico$C_Voice),]
ico.v<-ico[!is.na(ico$V_height) | !is.na(ico$V_backness),]
ico<-rbind(ico.v,ico.c)

# regression analyses (predicting Ico from SSt)

m1.v <- lm(log(Icon_fit-1) ~ St_StableShiftInGroup, data = ico.v)
summary(m1.v)

m1.c <- lm(log(Icon_fit-1) ~ St_StableShiftInGroup, data = ico.c)
summary(m1.c)

m2 <- lm(log(Icon_fit-1) ~ St_StableShiftInGroup, data=ico)
summary(m2)

# create subsets for the relevant major features (second analysis)

ico.p<-ico[!is.na(ico[, "C_PoA"]),]
ico.m<-ico[!is.na(ico[, "C_MoA"]),]
ico.vo<-ico[!is.na(ico[, "C_Voice"]),]
ico.h<-ico[!is.na(ico[, "V_height"]),]
ico.b<-ico[!is.na(ico[, "V_backness"]),]

# C_PoA plot (comparing Ico vs. SSt for consonants by place of articulation,
#             appears as left half of figure 3b in paper)

pdf("S6-result-graphs/c_poa.pdf", width = 7, height = 5)
ggplot(ico.p, aes(x=St_StableShiftInGroup, y=log(Icon_fit-1), color=C_PoA, shape=C_PoA)) +
    ggtitle("Place") +
    geom_point() +
    stat_density_2d(geom = "polygon",
                    aes(alpha = ..level.., fill = C_PoA),
                    bins = 4) +
    theme(legend.position=c(0.015,0.22), legend.justification=c(0,1)) +
    xlab('Sound_stability') + ylab('Log(iconic_value)') +
    scale_fill_discrete(name = 'C_Place',
                    breaks = c('E','L'),
                    labels = c('Earlier','Later')) +
    guides(alpha = F, color = F, shape = F)
dev.off()

# C_MoA plot (comparing Ico vs. SSt for consonants by manner of articulation, 
#             appears as recht half of figure 3a in paper)

pdf("S6-result-graphs/c_moa.pdf", width = 7, height = 5)
ggplot(ico.m, aes(x=St_StableShiftInGroup, y=log(Icon_fit-1), color=C_MoA, shape=C_MoA)) +
    ggtitle("Manner") +
    geom_point() +
    xlim(0.1,0.9) +
    ylim(-12.7,0) +
    stat_density_2d(geom = "polygon",
                    aes(alpha = ..level.., fill = C_MoA),
                    bins = 4) +
    theme(legend.position=c(0.015,0.22), legend.justification=c(0,1)) +
    xlab('Sound_stability') + ylab('Log(iconic_value)') +
    scale_fill_discrete(name = 'C_Manner',
                    breaks = c('E','L'),
                    labels = c('Earlier','Later')) +
    guides(alpha = F, color = F, shape = F)
dev.off()

# C_Voi plot (comparing Ico vs. SSt for consonants by voicing, supplementary materials only)

pdf("S6-result-graphs/c_voi.pdf", width = 7, height = 5)
ggplot(ico.vo, aes(x=St_StableShiftInGroup, y=log(Icon_fit-1), color=C_Voice, shape=C_Voice)) +
    ggtitle("Voicing") +
    geom_point() +
    xlim(0.1,0.9) +
    ylim(-12.7,0) +
    stat_density_2d(geom = "polygon",
                    aes(alpha = ..level.., fill = C_Voice),
                    bins = 4) +
    theme(legend.position=c(0.015,0.22), legend.justification=c(0,1)) +
    xlab('Sound_stability') + ylab('Log(iconic_value)') +
    scale_fill_discrete(name = 'C_Voice',
                        breaks = c('E','L'),
                        labels = c('Earlier','Later')) +
    guides(alpha = F, color = F, shape = F)
dev.off()

# V_Height plot (comparing Ico vs. SSt for vowels by height, supplementary materials only)

pdf("S6-result-graphs/v_height.pdf", width = 7, height = 5)
ggplot(ico.h, aes(x=St_StableShiftInGroup, y=log(Icon_fit-1), color=V_height, shape=V_height)) +
    ggtitle("Height") +
    geom_point() +
    xlim(0.1,0.9) +
    ylim(-12.7,0) +
    stat_density_2d(geom = "polygon",
                    aes(alpha = ..level.., fill = V_height),
                    bins = 4) +
    theme(legend.position=c(0.015,0.22), legend.justification=c(0,1)) +
    xlab('Sound_stability') + ylab('Log(iconic_value)') +
    scale_fill_discrete(name = 'V_height',
                        breaks = c('E','L'),
                        labels = c('Earlier','Later')) +
    guides(alpha = F, color = F, shape = F)
dev.off()

# V_Back plot (comparing Ico vs. SSt for vowels by backness, supplementary materials only)

pdf("S6-result-graphs/v_back.pdf", width = 7, height = 5)
ggplot(ico.b, aes(x=St_StableShiftInGroup, y=log(Icon_fit-1), color=V_backness, shape=V_backness)) +
    ggtitle("Backness") +
    geom_point() +
    xlim(0.1,0.9) +
    ylim(-12.7,0) +
    stat_density_2d(geom = "polygon",
                    aes(alpha = ..level.., fill = V_backness),
                    bins = 4) +
    theme(legend.position=c(0.015,0.22), legend.justification=c(0,1)) +
    xlab('Sound_stability') + ylab('Log(iconic_value)') +
    scale_fill_discrete(name = 'V_backness',
                        breaks = c('E','L'),
                        labels = c('Earlier','Later')) +
    guides(alpha = F, color = F, shape = F)
dev.off()
