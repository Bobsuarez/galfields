package co.com.galfields.pos_transactions.usecase.catalog;

import co.com.galfields.pos_transactions.model.ResourceNotFoundException;
import co.com.galfields.pos_transactions.model.catalog.Category;
import co.com.galfields.pos_transactions.model.catalog.gateways.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new CategoryUseCase(categoryRepository);
    }

    @Test
    void createSavesAndReturnsCategory() {
        Category toSave = Category.builder().name("Bebidas").description("desc").build();
        Category saved = Category.builder().categoryId(1L).name("Bebidas").description("desc").build();
        when(categoryRepository.save(toSave)).thenReturn(saved);

        assertThat(useCase.create(toSave)).isEqualTo(saved);
    }

    @Test
    void getThrowsNotFoundWhenMissing() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.get(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsAllOrderedByName() {
        List<Category> categories = List.of(Category.builder().categoryId(1L).name("Bebidas").build());
        when(categoryRepository.findAllOrderByName()).thenReturn(categories);

        assertThat(useCase.list()).isEqualTo(categories);
    }

    @Test
    void updateMutatesExistingFieldsOnly() {
        Category existing = Category.builder().categoryId(1L).name("Old").description("old desc").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Category patch = Category.builder().name("New").description("new desc").build();
        Category result = useCase.update(1L, patch);

        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("new desc");
    }

    @Test
    void deleteChecksExistenceFirst() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(Category.builder().categoryId(1L).build()));

        useCase.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsNotFoundWhenMissing() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.delete(404L)).isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, org.mockito.Mockito.never()).deleteById(any());
    }
}
