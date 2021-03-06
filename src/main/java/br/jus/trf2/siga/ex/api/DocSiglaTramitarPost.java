package br.jus.trf2.siga.ex.api;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.ex.ExDocumento;
import br.gov.jfrj.siga.ex.ExMobil;
import br.gov.jfrj.siga.ex.ExPapel;
import br.gov.jfrj.siga.ex.bl.Ex;
import br.gov.jfrj.siga.persistencia.ExMobilDaoFiltro;
import br.jus.trf2.siga.ex.api.ISigaDoc.DocSiglaTramitarPostRequest;
import br.jus.trf2.siga.ex.api.ISigaDoc.DocSiglaTramitarPostResponse;
import br.jus.trf2.siga.ex.api.ISigaDoc.IDocSiglaTramitarPost;
import br.jus.trf2.siga.ex.api.TokenCriarPost.Usuario;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class DocSiglaTramitarPost implements IDocSiglaTramitarPost {

	@Override
	public void run(DocSiglaTramitarPostRequest req,
			DocSiglaTramitarPostResponse resp) throws Exception {
		String authorization = TokenCriarPost.assertAuthorization();
		Usuario u = TokenCriarPost.assertUsuario();

		try (ExDB db = ExDB.create(true)) {
			try {
				DpPessoa cadastrante = db.getPessoaPorPrincipal(u.usuario);
				DpLotacao lotaCadastrante = cadastrante.getLotacao();
				DpPessoa titular = cadastrante;
				DpLotacao lotaTitular = cadastrante.getLotacao();

				ExMobilDaoFiltro flt = new ExMobilDaoFiltro();
				flt.setSigla(req.sigla);
				ExMobil mob = db.consultarPorSigla(flt);
				ExDocumento doc = mob.doc();

				DpLotacao lot = new DpLotacao();
				lot.setSigla(req.lotacao);
				lot = db.consultarPorSigla(lot);

				DpPessoa pes = new DpPessoa();
				pes.setSigla(req.matricula);
				pes = db.consultarPorSigla(pes);

				Utils.assertAcesso(mob, titular, lotaTitular);

				if (!Ex.getInstance().getComp()
						.podeTransferir(titular, lotaTitular, mob))
					throw new Exception("O documento " + req.sigla
							+ " não pode ser tramitado por "
							+ titular.getSiglaCompleta() + "/"
							+ lotaTitular.getSiglaCompleta());

				Ex.getInstance()
						.getBL()
						.transferir(null, null, cadastrante, lotaCadastrante,
								mob, null, null, null, lot, pes, null, null,
								null, titular, null, true, null, null, null,
								false, false);
				db.commit();
				resp.status = "OK";
			} catch (Exception ex) {
				db.rollback(ex);
			}
		}
	}

	@Override
	public String getContext() {
		return "tramitar documento";
	}

}
