package com.ashokvarma.androidx.recyclerview.selection

import android.R
import android.R.id
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView

import com.ashokvarma.androidx.R
import com.ashokvarma.androidx.util.ResourcesUtils
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView

import android.R.attr.name
import android.support.test.espresso.matcher.ViewMatchers.isSelected
import android.support.v4.view.ViewCompat.setActivated

/**
 * Class description
 *
 * @author ashok
 * @version 1.0
 * @since 16/06/18
 */
class PokemonRecyclerAdapter internal constructor(context: Context, private val mPokemons: List<Pokemon>) : RecyclerView.Adapter<PokemonRecyclerAdapter.PokemonViewHolder>() {
    private val mLayoutInflater: LayoutInflater
    private val selectedItemMarginInPx: Int
    @Nullable
    private var mSelectionTracker: SelectionTracker<String>? = null
    @Nullable
    private var mPokemonClickListener: PokemonClickListener? = null

    init {
        mLayoutInflater = LayoutInflater.from(context)
        selectedItemMarginInPx = context.resources.getDimensionPixelSize(R.dimen.selected_item_margin)
    }

    fun setSelectionTracker(selectionTracker: SelectionTracker<String>) {
        this.mSelectionTracker = selectionTracker
    }

    fun setPokemonClickListener(@Nullable pokemonClickListener: PokemonClickListener) {
        this.mPokemonClickListener = pokemonClickListener
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): PokemonViewHolder {
        return PokemonViewHolder(mLayoutInflater.inflate(R.layout.pokemon_list_item, viewGroup, false), selectedItemMarginInPx)
    }

    override fun onBindViewHolder(pokemonViewHolder: PokemonViewHolder, position: Int) {
        val pokemon = mPokemons[position]

        var isSelected = false
        if (mSelectionTracker != null) {
            if (mSelectionTracker!!.isSelected(pokemon.id)) {
                isSelected = true
            }
        }

        pokemonViewHolder.bind(position, isSelected, pokemon)
    }

    override fun getItemCount(): Int {
        return mPokemons.size
    }

    internal inner class PokemonViewHolder(itemView: View, selectedItemMarginInPx: Int) : RecyclerView.ViewHolder(itemView) {
        val numberView: TextView
        val nameView: TextView
        val typeView: TextView
        val checkboxView: CheckBox
        val imageView: ImageView
        val bgColorView: View
        private val pokemonItemDetails: PokemonItemDetails
        private var pokemon: Pokemon? = null

        init {
            numberView = itemView.findViewById(R.id.pokemon_list_number)
            nameView = itemView.findViewById(R.id.pokemon_list_name)
            typeView = itemView.findViewById(R.id.pokemon_list_type)
            checkboxView = itemView.findViewById(R.id.pokemon_list_checkbox)
            imageView = itemView.findViewById(R.id.pokemon_list_image)
            bgColorView = itemView.findViewById(R.id.pokemon_list_background)
            pokemonItemDetails = PokemonItemDetails()

            itemView.setOnClickListener {
                if (mPokemonClickListener != null) {
                    mPokemonClickListener!!.onCLick(pokemon)
                }
            }
        }

        fun bind(position: Int, isSelected: Boolean, pokemon: Pokemon) {
            this.pokemon = pokemon
            pokemonItemDetails.position = position
            pokemonItemDetails.selectionKey = pokemon.id

            numberView.text = "#" + pokemon.number
            nameView.setText(pokemon.name)
            typeView.setText(pokemon.type)

            bgColorView.setBackgroundColor(pokemon.bgColor)
            imageView.setImageResource(ResourcesUtils.getDrawResIdentifier("pokemon_" + pokemon.number))

            checkboxView.isChecked = isSelected
            itemView.isActivated = isSelected
            if (isSelected) {
                bgColorView.alpha = 0.5f
                (bgColorView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(selectedItemMarginInPx, selectedItemMarginInPx, selectedItemMarginInPx, selectedItemMarginInPx)
            } else {
                bgColorView.alpha = 0.1f
                (bgColorView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0, 0, 0, 0)
            }
        }

        fun getPokemonItemDetails(motionEvent: MotionEvent): ItemDetailsLookup.ItemDetails<String> {
            return pokemonItemDetails
        }
    }

    internal class PokemonItemDetails : ItemDetailsLookup.ItemDetails<String>() {
        var position: Int = 0
        @get:Nullable
        var selectionKey: String? = null

        fun inSelectionHotspot(e: MotionEvent): Boolean {
            return true
        }

        fun inDragRegion(e: MotionEvent): Boolean {
            return true
        }
    }

    internal interface PokemonClickListener {
        fun onCLick(pokemon: Pokemon?)
    }
}