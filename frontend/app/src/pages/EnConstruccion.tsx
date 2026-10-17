interface Props {
  titulo: string;
  historia: string;
  sprint: string;
}

/** Placeholder for screens planned in later sprints (see docs/09-plan-construccion.md). */
export function EnConstruccion({ titulo, historia, sprint }: Props) {
  return (
    <section className="tarjeta">
      <h1>{titulo}</h1>
      <p>
        Pantalla planificada para el sprint {sprint} ({historia}).
      </p>
    </section>
  );
}
