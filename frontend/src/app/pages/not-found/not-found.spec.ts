import { TestBed } from '@angular/core/testing';
import { NotFoundPage } from './not-found';

describe('NotFoundPage', () => {
  it('renders the route-not-found message', async () => {
    await TestBed.configureTestingModule({
      imports: [NotFoundPage],
    }).compileComponents();

    const fixture = TestBed.createComponent(NotFoundPage);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ruta no encontrada');
  });
});
