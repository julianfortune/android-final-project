package com.cs492.cocktailapp.ui.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cs492.cocktailapp.R
import com.cs492.cocktailapp.data.BrowseCategory
import com.cs492.cocktailapp.data.CocktailListItemSize
import com.cs492.cocktailapp.data.LoadingStatus
import com.cs492.cocktailapp.ui.list.CocktailListAdapter

/*
 * The caller *MUST* put a com.cs492.cocktailapp.data.BrowseCategory with key CATEGORY_ARGUMENT in
 * arguments immediately after instantiation or else a crash will occur.
 */
class BrowseFragment : Fragment() {

    private lateinit var category: BrowseCategory
    private lateinit var pageViewModel: BrowseViewModel

    private lateinit var recyclerView: RecyclerView
    private val adapter = CocktailListAdapter(CocktailListItemSize.Large)

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var errorView: View
    private lateinit var showSavedButton: Button
    private lateinit var errorHeadline: TextView
    private lateinit var errorDetail: TextView

    // Source: "Kotlin Fragment to Activity Communication Example" by zacharymikel
    // https://gist.github.com/zacharymikel/40aa61b2ff4d0b1ae267212d7dd965e5
    private var listener: CocktailFragmentListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is CocktailFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context does not implement `BrowseFragmentListener")
        }
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚠️ This will crash if the argument is not passed
        val category = requireArguments().getSerializable(CATEGORY_ARGUMENT) as BrowseCategory
        this.category = category

        // There is a different ViewModel for each Fragment based on the category.
        pageViewModel = ViewModelProvider(this).get(category.toString(), BrowseViewModel::class.java).apply {
            setCategory(category)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_browse, container, false)

        // Grab all the UI elements
        swipeRefreshLayout = root.findViewById(R.id.browseSwipeRefreshLayout)
        recyclerView = root.findViewById(R.id.browseRecyclerView)
        errorView = root.findViewById(R.id.browseErrorView)
        errorHeadline = root.findViewById(R.id.browseErrorHeadline)
        errorDetail = root.findViewById(R.id.browseErrorExplanation)
        showSavedButton = root.findViewById(R.id.showSavedButton)

        // Set up recycler view
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        // Click listener
        adapter.onClickHandler = listener!!::navigateTo

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            pageViewModel.loadBrowseItems()
        }

        // Connect view model to the recycler view
        pageViewModel.browseItems.observe(viewLifecycleOwner, {
            adapter.cocktailRecipeList = it
        })

        // Observe view model status
        pageViewModel.loadingStatus.observe(viewLifecycleOwner, {
            it?.let { newStatus ->
                when (newStatus) {
                    LoadingStatus.Error -> {
                        swipeRefreshLayout.isRefreshing = false

                        recyclerView.visibility = View.INVISIBLE
                        errorView.visibility = View.VISIBLE

                        errorHeadline.text = getString(R.string.browse_error_no_connection)
                        errorDetail.text = getString(R.string.browse_error_no_connection_saved_suggestion)
                    }
                    LoadingStatus.Loading -> {
                        if (!swipeRefreshLayout.isRefreshing) {
                            swipeRefreshLayout.isRefreshing = true
                        }
                    }
                    LoadingStatus.Success -> {
                        swipeRefreshLayout.isRefreshing = false

                        recyclerView.visibility = View.VISIBLE
                        errorView.visibility = View.INVISIBLE
                    }
                }
            } ?: run {
                Log.w(TAG, "`loadingStatus` is null!")
            }
        })

        showSavedButton.setOnClickListener {
            listener?.showSavedCocktails()
        }

        return root
    }

    companion object {
        private val TAG = this::class.simpleName

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val CATEGORY_ARGUMENT = "category"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(category: BrowseCategory): BrowseFragment {
            return BrowseFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(CATEGORY_ARGUMENT, category)
                }
            }
        }
    }
}