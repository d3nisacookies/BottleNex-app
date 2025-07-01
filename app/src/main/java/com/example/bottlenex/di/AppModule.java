package com.example.bottlenex.di;

import android.content.Context;

import com.example.bottlenex.map.MapManager;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    
    @Provides
    @Singleton
    public MapManager provideMapManager(@ApplicationContext Context context) {
        return new MapManager(context);
    }
} 