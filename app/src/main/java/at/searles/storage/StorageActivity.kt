package com.ashokvarma.androidx.recyclerview.selection

import android.R
import android.R.id
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MotionEvent
import android.view.View

import com.ashokvarma.androidx.R

import java.util.HashMap
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.nio.file.Files.size


class RecyclerSelectionActivity : AppCompatActivity() {

    internal var selectionTracker: SelectionTracker<String>
    internal var toolbarView: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.support_recycler_selection_act)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        toolbarView = findViewById<View>(R.id.tool_bar)

        val pokemonData = Pokemon.catchThemAll()
        val pokemonRecyclerAdapter = PokemonRecyclerAdapter(this, pokemonData)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        // adapter must be set before building selectionTracker
        recyclerView.adapter = pokemonRecyclerAdapter
        recyclerView.addItemDecoration(SpacesItemDecoration(this, R.dimen.pokemon_item_spacing))

        selectionTracker = SelectionTracker.Builder(
                "pokemon-selection", //unique id
                recyclerView,
                PokemonItemKeyProvider(pokemonData),
                PokemonItemDetailsLookup(recyclerView),
                StorageStrategy.createStringStorage())
                .build()

        pokemonRecyclerAdapter.setSelectionTracker(selectionTracker)
        //        pokemonRecyclerAdapter.setPokemonClickListener(new PokemonRecyclerAdapter.PokemonClickListener() {
        //            @Override
        //            public void onCLick(Pokemon pokemon) {
        //                Toast.makeText(RecyclerSelectionActivity.this, pokemon.name + " clicked", Toast.LENGTH_LONG).show();
        //            }
        //        });

        setUpViews()
    }

    internal fun setUpViews() {
        toolbarView.setNavigationOnClickListener(View.OnClickListener { selectionTracker.clearSelection() })

        updateViewsBasedOnSelection()
        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
            fun onSelectionChanged() {
                updateViewsBasedOnSelection()
                super.onSelectionChanged()
            }

            fun onSelectionRestored() {
                updateViewsBasedOnSelection()
                super.onSelectionRestored()
            }
        })
    }

    private fun updateViewsBasedOnSelection() {
        if (selectionTracker.hasSelection()) {
            toolbarView.setVisibility(View.VISIBLE)
            toolbarView.setTitle(selectionTracker.getSelection().size() + " selected")
        } else {
            toolbarView.setVisibility(View.GONE)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectionTracker.onRestoreInstanceState(savedInstanceState)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private class PokemonItemKeyProvider internal constructor(private val mPokemonList: List<Pokemon>) : ItemKeyProvider<String>(SCOPE_CACHED) {

        private val mKeyToPosition: MutableMap<String, Int>

        init {

            mKeyToPosition = HashMap(mPokemonList.size)
            var i = 0
            for (pokemon in mPokemonList) {
                mKeyToPosition[pokemon.id] = i
                i++
            }
        }

        @Nullable
        fun getKey(i: Int): String {
            return mPokemonList[i].id// directly from position to key
        }

        fun getPosition(s: String): Int {
            return mKeyToPosition[s]!!
        }
    }

    private class PokemonItemDetailsLookup internal constructor(internal var mRecyclerView: RecyclerView) : ItemDetailsLookup<String>() {

        @Nullable
        fun getItemDetails(motionEvent: MotionEvent): ItemDetails<String>? {
            val view = mRecyclerView.findChildViewUnder(motionEvent.x, motionEvent.y)
            if (view != null) {
                val viewHolder = mRecyclerView.getChildViewHolder(view)
                //                int position = viewHolder.getAdapterPosition();
                if (viewHolder is PokemonRecyclerAdapter.PokemonViewHolder) {
                    return viewHolder.getPokemonItemDetails(motionEvent)
                }
            }
            return null
        }
    }

    override fun onBackPressed() {
        if (selectionTracker.hasSelection()) {
            selectionTracker.clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}