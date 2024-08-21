package com.example.animalpredictor

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class CreatureLearn : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var animalRecyclerView: RecyclerView
    private lateinit var messageTextView: TextView
    private lateinit var animalAdapter: AnimalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creature_learn)

        searchEditText = findViewById(R.id.searchEditText)
        animalRecyclerView = findViewById(R.id.animalRecyclerView)
        messageTextView = findViewById(R.id.messageTextView)

        animalRecyclerView.layoutManager = LinearLayoutManager(this)
        animalAdapter = AnimalAdapter(emptyList())
        animalRecyclerView.adapter = animalAdapter

        searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            ) {
                performSearch()
                true
            } else {
                false
            }
        }


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }

                R.id.navigation_learn -> true
                R.id.navigation_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                    true
                }

                else -> false
            }
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        if (query.isNotEmpty()) {
            searchAnimals(query)
        } else {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchAnimals(name: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.api-ninjas.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().addInterceptor { chain ->
                val original = chain.request()
                val request: Request = original.newBuilder()
                    .header("X-Api-Key", "/kWhuVGQvUdYfjRd177faw==6msc6qDlZ15C2cUQ")
                    .method(original.method(), original.body())
                    .build()
                chain.proceed(request)
            }.build())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val call = apiService.searchAnimals(name)
        call.enqueue(object : retrofit2.Callback<List<AnimalResponse>> {
            override fun onResponse(
                call: retrofit2.Call<List<AnimalResponse>>,
                response: retrofit2.Response<List<AnimalResponse>>
            ) {
                if (response.isSuccessful) {
                    val animalResponses = response.body()
                    if (animalResponses.isNullOrEmpty()) {
                        showNoResultsMessage()
                    } else {
                        val animals = animalResponses.map { animalResponse ->
                            Animal(
                                name = animalResponse.name ?: "N/A",
                                taxonomy = animalResponse.taxonomy ?: Taxonomy(),
                                locations = animalResponse.locations ?: emptyList(),
                                characteristics = animalResponse.characteristics
                                    ?: Characteristics()
                            )
                        }
                        updateUIWithAnimals(animals)
                    }
                } else {
                    showError("No results found. Please try a different search term.")
                }
            }

            override fun onFailure(call: retrofit2.Call<List<AnimalResponse>>, t: Throwable) {
                t.printStackTrace()
                showError("Error fetching data. Please check your connection and try again.")
            }
        })
    }

    private fun showNoResultsMessage() {
        animalAdapter.updateAnimals(emptyList())
        messageTextView.visibility = View.VISIBLE
        messageTextView.text = "No results found"
    }

    private fun updateUIWithAnimals(animals: List<Animal>) {
        animalAdapter.updateAnimals(animals)
        messageTextView.visibility = View.GONE
    }

    private fun showError(message: String) {
        animalAdapter.updateAnimals(emptyList())
        messageTextView.visibility = View.VISIBLE
        messageTextView.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    data class Animal(
        val name: String,
        val taxonomy: Taxonomy,
        val locations: List<String>,
        val characteristics: Characteristics
    )

    data class Taxonomy(
        val kingdom: String? = "N/A",
        val phylum: String? = "N/A",
        val `class`: String? = "N/A",
        val order: String? = "N/A",
        val family: String? = "N/A",
        val genus: String? = "N/A",
        val scientific_name: String? = "N/A"
    )

    data class Characteristics(
        val prey: String? = "N/A",
        val name_of_young: String? = "N/A",
        val group_behavior: String? = "N/A",
        val estimated_population_size: String? = "N/A",
        val biggest_threat: String? = "N/A",
        val most_distinctive_feature: String? = "N/A",
        val other_name: String? = "N/A",
        val gestation_period: String? = "N/A",
        val habitat: String? = "N/A",
        val predators: String? = "N/A",
        val diet: String? = "N/A",
        val average_litter_size: String? = "N/A",
        val lifestyle: String? = "N/A",
        val common_name: String? = "N/A",
        val number_of_species: String? = "N/A",
        val location: String? = "N/A",
        val slogan: String? = "N/A",
        val group: String? = "N/A",
        val color: String? = "N/A",
        val skin_type: String? = "N/A",
        val top_speed: String? = "N/A",
        val lifespan: String? = "N/A",
        val weight: String? = "N/A",
        val height: String? = "N/A",
        val age_of_sexual_maturity: String? = "N/A",
        val age_of_weaning: String? = "N/A"
    )

    class AnimalAdapter(private var animals: List<Animal>) :
        RecyclerView.Adapter<AnimalAdapter.AnimalViewHolder>() {

        inner class AnimalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.animalNameTextView)
            val scientificNameTextView: TextView =
                itemView.findViewById(R.id.scientificNameTextView)
            val taxonomyTextView: TextView = itemView.findViewById(R.id.taxonomyTextView)
            val locationsTextView: TextView = itemView.findViewById(R.id.locationsTextView)
            val characteristicsTextView: TextView =
                itemView.findViewById(R.id.characteristicsTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_animal, parent, false)
            return AnimalViewHolder(view)
        }

        override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
            val animal = animals[position]
            holder.nameTextView.text = animal.name
            holder.scientificNameTextView.text =
                "Scientific Name: ${animal.taxonomy.scientific_name}"
            holder.taxonomyTextView.text = """
                Kingdom: ${animal.taxonomy.kingdom}
                Phylum: ${animal.taxonomy.phylum}
                Class: ${animal.taxonomy.`class`}
                Order: ${animal.taxonomy.order}
                Family: ${animal.taxonomy.family}
                Genus: ${animal.taxonomy.genus}
            """.trimIndent()



            holder.locationsTextView.text = "Locations: ${animal.locations.joinToString(", ")}"
            holder.characteristicsTextView.text = """
                Prey: ${animal.characteristics.prey}
                Group Behavior: ${animal.characteristics.group_behavior}
                Habitat: ${animal.characteristics.habitat}
                Diet: ${animal.characteristics.diet}
                Lifestyle: ${animal.characteristics.lifestyle}
                Color: ${animal.characteristics.color}
                Skin Type: ${animal.characteristics.skin_type}
                Top Speed: ${animal.characteristics.top_speed}
                Lifespan: ${animal.characteristics.lifespan}
                Weight: ${animal.characteristics.weight}
                Height: ${animal.characteristics.height}
                """.trimIndent()

        }

        override fun getItemCount() = animals.size

        fun updateAnimals(newAnimals: List<Animal>) {
            animals = newAnimals
            notifyDataSetChanged()
        }
    }

    interface ApiService {
        @GET("animals")
        fun searchAnimals(@Query("name") name: String): retrofit2.Call<List<AnimalResponse>>
    }

    data class AnimalResponse(
        val name: String?,
        val taxonomy: Taxonomy?,
        val locations: List<String>?,
        val characteristics: Characteristics?
    )
}