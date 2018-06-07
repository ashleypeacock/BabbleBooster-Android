package com.divadvo.babbleboosternew.data;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.divadvo.babbleboosternew.data.model.response.Pokemon;
import com.divadvo.babbleboosternew.data.remote.PokemonService;
import io.reactivex.Single;

/**
 * Created by shivam on 29/5/17.
 */
@Singleton
public class DataManager {

    private PokemonService pokemonService;

    @Inject
    public DataManager(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    public Single<List<String>> getPokemonList(int limit) {
        Single<List<String>> list = pokemonService
                .getPokemonList(limit)
                .toObservable()
                .flatMapIterable(namedResources -> namedResources.results)
                .map(namedResource -> namedResource.name)
                .toList();
        return list;
    }

    public Single<Pokemon> getPokemon(String name) {
        return pokemonService.getPokemon(name);
    }
}
